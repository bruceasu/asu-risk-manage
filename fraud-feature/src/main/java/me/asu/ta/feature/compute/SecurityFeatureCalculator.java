package me.asu.ta.feature.compute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SecurityFeatureCalculator implements FeatureCalculator {
    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_7D = Duration.ofDays(7);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SecurityFeatureCalculator(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void enrichBatch(
            List<String> accountIds,
            Instant generatedAt,
            int featureVersion,
            Map<String, AccountFeatureSnapshot.Builder> builders) {
        if (accountIds.isEmpty()) {
            return;
        }
        applySecurityEventMetrics(accountIds, generatedAt, builders);
        applySecurityBeforeWithdrawMetric(accountIds, generatedAt, builders);
    }

    private void applySecurityEventMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with profile_changes as (
                    select se.account_id,
                           count(*) filter (where se.event_type in ('email_change', 'phone_change', 'profile_change'))::int as profile_change_count_24h,
                           count(*)::int as security_event_count_24h
                      from security_events se
                     where se.account_id in (:accountIds)
                       and se.created_at > :windowStart
                       and se.created_at <= :generatedAt
                     group by se.account_id
                ),
                password_changes as (
                    select pr.account_id,
                           count(*)::int as password_change_count_7d
                      from password_resets pr
                     where pr.account_id in (:accountIds)
                       and pr.reset_time > :passwordWindowStart
                       and pr.reset_time <= :generatedAt
                     group by pr.account_id
                )
                select coalesce(pc.account_id, pw.account_id) as account_id,
                       pc.security_event_count_24h,
                       case when coalesce(pc.profile_change_count_24h, 0) >= 2 then true else false end as rapid_profile_change_flag_24h
                  from profile_changes pc
                  full outer join password_changes pw on pw.account_id = pc.account_id
                """;
        var params = FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H)
                .addValue("passwordWindowStart", java.sql.Timestamp.from(generatedAt.minus(WINDOW_7D)));
        jdbcTemplate.query(
                sql,
                params,
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.securityEventCount24h(FeatureCalculatorSupport.getInteger(rs, "security_event_count_24h"));
                    builder.rapidProfileChangeFlag24h(FeatureCalculatorSupport.getBoolean(rs, "rapid_profile_change_flag_24h"));
                }));
    }

    private void applySecurityBeforeWithdrawMetric(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with latest_security_event as (
                    select se.account_id,
                           max(se.created_at) as latest_security_event_time
                      from security_events se
                     where se.account_id in (:accountIds)
                       and se.created_at > :windowStart
                       and se.created_at <= :generatedAt
                     group by se.account_id
                )
                select w.account_id,
                       max(case when lse.latest_security_event_time is not null
                                     and lse.latest_security_event_time <= w.created_at
                                     and w.created_at - lse.latest_security_event_time <= interval '2 hours'
                                then 1 else 0 end)::boolean as security_change_before_withdraw_flag_24h
                  from withdrawals w
                  left join latest_security_event lse on lse.account_id = w.account_id
                 where w.account_id in (:accountIds)
                   and w.created_at > :windowStart
                   and w.created_at <= :generatedAt
                 group by w.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.securityChangeBeforeWithdrawFlag24h(
                            FeatureCalculatorSupport.getBoolean(rs, "security_change_before_withdraw_flag_24h"));
                }));
    }

    private RowCallbackHandler callback(SqlConsumer consumer) {
        return rs -> consumer.accept(rs);
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(ResultSet rs) throws SQLException;
    }
}
