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
public class LoginFeatureCalculator implements FeatureCalculator {
    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_7D = Duration.ofDays(7);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LoginFeatureCalculator(NamedParameterJdbcTemplate jdbcTemplate) {
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
        applyLoginMetrics(accountIds, generatedAt, builders);
        applyNewDeviceAndNightMetrics(accountIds, generatedAt, builders);
    }

    private void applyLoginMetrics(
            List<String> accountIds,
            Instant generatedAt,
            Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                select ll.account_id,
                       count(*)::int as login_count_24h,
                       sum(case when ll.success = false then 1 else 0 end)::int as login_failure_count_24h,
                       case when count(*) = 0 then 0.0
                            else sum(case when ll.success = false then 1 else 0 end)::double precision / count(*)
                       end as login_failure_rate_24h,
                       count(distinct ll.ip)::int as unique_ip_count_24h,
                       sum(case when ii.risk_level in ('HIGH', 'CRITICAL') then 1 else 0 end)::int as high_risk_ip_login_count_24h,
                       sum(case when coalesce(ii.is_vpn, false) or coalesce(ii.is_proxy, false) then 1 else 0 end)::int as vpn_ip_login_count_24h
                  from login_logs ll
                  left join ip_intelligence ii on ii.ip = ll.ip
                 where ll.account_id in (:accountIds)
                   and ll.login_time > :windowStart
                   and ll.login_time <= :generatedAt
                 group by ll.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.loginCount24h(FeatureCalculatorSupport.getInteger(rs, "login_count_24h"));
                    builder.loginFailureCount24h(FeatureCalculatorSupport.getInteger(rs, "login_failure_count_24h"));
                    builder.loginFailureRate24h(FeatureCalculatorSupport.getDouble(rs, "login_failure_rate_24h"));
                    builder.uniqueIpCount24h(FeatureCalculatorSupport.getInteger(rs, "unique_ip_count_24h"));
                    builder.highRiskIpLoginCount24h(FeatureCalculatorSupport.getInteger(rs, "high_risk_ip_login_count_24h"));
                    builder.vpnIpLoginCount24h(FeatureCalculatorSupport.getInteger(rs, "vpn_ip_login_count_24h"));
                }));
    }

    private void applyNewDeviceAndNightMetrics(
            List<String> accountIds,
            Instant generatedAt,
            Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                select ll.account_id,
                       sum(case when ad.first_seen > :windowStart then 1 else 0 end)::int as new_device_login_count_7d,
                       case when count(*) = 0 then 0.0
                            else sum(case when extract(hour from ll.login_time) between 0 and 5 then 1 else 0 end)::double precision / count(*)
                       end as night_login_ratio_7d
                  from login_logs ll
                  left join account_devices ad
                    on ad.account_id = ll.account_id
                   and ad.device_id = ll.device_id
                 where ll.account_id in (:accountIds)
                   and ll.login_time > :windowStart
                   and ll.login_time <= :generatedAt
                 group by ll.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_7D),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.newDeviceLoginCount7d(FeatureCalculatorSupport.getInteger(rs, "new_device_login_count_7d"));
                    builder.nightLoginRatio7d(FeatureCalculatorSupport.getDouble(rs, "night_login_ratio_7d"));
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
