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
public class DeviceFeatureCalculator implements FeatureCalculator {
    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_7D = Duration.ofDays(7);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeviceFeatureCalculator(NamedParameterJdbcTemplate jdbcTemplate) {
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
        applyDeviceFootprintMetrics(accountIds, generatedAt, builders);
        applySwitchMetrics(accountIds, generatedAt, builders);
    }

    private void applyDeviceFootprintMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                select ad.account_id,
                       count(distinct ad.device_id)::int as unique_device_count_7d,
                       sum(shared.shared_account_count)::int as shared_device_accounts_7d
                  from account_devices ad
                  left join (
                        select ad2.device_id,
                               greatest(count(distinct ad2.account_id) - 1, 0)::int as shared_account_count
                          from account_devices ad2
                         where ad2.last_seen > :windowStart
                           and ad2.last_seen <= :generatedAt
                         group by ad2.device_id
                  ) shared on shared.device_id = ad.device_id
                 where ad.account_id in (:accountIds)
                   and ad.last_seen > :windowStart
                   and ad.last_seen <= :generatedAt
                 group by ad.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_7D),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.uniqueDeviceCount7d(FeatureCalculatorSupport.getInteger(rs, "unique_device_count_7d"));
                    builder.sharedDeviceAccounts7d(FeatureCalculatorSupport.getInteger(rs, "shared_device_accounts_7d"));
                }));
    }

    private void applySwitchMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with ordered_logins as (
                    select ll.account_id,
                           ll.device_id,
                           lag(ll.device_id) over (partition by ll.account_id order by ll.login_time) as previous_device_id
                      from login_logs ll
                     where ll.account_id in (:accountIds)
                       and ll.login_time > :windowStart
                       and ll.login_time <= :generatedAt
                )
                select account_id,
                       sum(case when previous_device_id is not null and previous_device_id <> device_id then 1 else 0 end)::int
                           as device_switch_count_24h
                  from ordered_logins
                 group by account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.deviceSwitchCount24h(FeatureCalculatorSupport.getInteger(rs, "device_switch_count_24h"));
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
