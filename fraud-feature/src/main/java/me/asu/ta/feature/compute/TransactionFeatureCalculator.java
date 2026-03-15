package me.asu.ta.feature.compute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionFeatureCalculator implements FeatureCalculator {
    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_30D = Duration.ofDays(30);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TransactionFeatureCalculator(NamedParameterJdbcTemplate jdbcTemplate) {
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
        applyTransactionMetrics(accountIds, generatedAt, builders);
        applyDepositWithdrawalMetrics(accountIds, generatedAt, builders);
        applyRewardMetrics(accountIds, generatedAt, builders);
    }

    private void applyTransactionMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                select t.account_id,
                       count(*)::int as transaction_count_24h,
                       coalesce(sum(t.amount), 0)::double precision as total_amount_24h,
                       coalesce(avg(t.amount), 0)::double precision as avg_transaction_amount_24h,
                       count(distinct t.counterparty_account)::int as unique_counterparty_count_24h
                  from transactions t
                 where t.account_id in (:accountIds)
                   and t.created_at > :windowStart
                   and t.created_at <= :generatedAt
                 group by t.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.transactionCount24h(FeatureCalculatorSupport.getInteger(rs, "transaction_count_24h"));
                    builder.totalAmount24h(FeatureCalculatorSupport.getDouble(rs, "total_amount_24h"));
                    builder.avgTransactionAmount24h(FeatureCalculatorSupport.getDouble(rs, "avg_transaction_amount_24h"));
                    builder.uniqueCounterpartyCount24h(FeatureCalculatorSupport.getInteger(rs, "unique_counterparty_count_24h"));
                }));
    }

    private void applyDepositWithdrawalMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with deposit_stats as (
                    select d.account_id,
                           count(*)::int as deposit_count_24h,
                           coalesce(sum(d.amount), 0)::double precision as deposit_amount_24h,
                           max(d.created_at) as last_deposit_time
                      from deposits d
                     where d.account_id in (:accountIds)
                       and d.created_at > :windowStart
                       and d.created_at <= :generatedAt
                     group by d.account_id
                ),
                withdraw_stats as (
                    select w.account_id,
                           count(*)::int as withdraw_count_24h,
                           coalesce(sum(w.amount), 0)::double precision as withdraw_amount_24h,
                           avg(extract(epoch from (w.created_at - ds.last_deposit_time)) / 60.0)::double precision
                               as withdraw_after_deposit_delay_avg_24h,
                           max(case when ds.last_deposit_time is not null and w.created_at - ds.last_deposit_time <= interval '30 minutes'
                                    then 1 else 0 end)::boolean as rapid_withdraw_after_deposit_flag_24h
                      from withdrawals w
                      left join deposit_stats ds on ds.account_id = w.account_id
                     where w.account_id in (:accountIds)
                       and w.created_at > :windowStart
                       and w.created_at <= :generatedAt
                     group by w.account_id
                )
                select coalesce(ds.account_id, ws.account_id) as account_id,
                       ds.deposit_count_24h,
                       ds.deposit_amount_24h,
                       ws.withdraw_count_24h,
                       ws.withdraw_amount_24h,
                       case when coalesce(ds.deposit_amount_24h, 0) = 0 then null
                            else coalesce(ws.withdraw_amount_24h, 0) / ds.deposit_amount_24h
                       end as deposit_withdraw_ratio_24h,
                       ws.withdraw_after_deposit_delay_avg_24h,
                       coalesce(ws.rapid_withdraw_after_deposit_flag_24h, false) as rapid_withdraw_after_deposit_flag_24h
                  from deposit_stats ds
                  full outer join withdraw_stats ws on ws.account_id = ds.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_24H),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.depositCount24h(FeatureCalculatorSupport.getInteger(rs, "deposit_count_24h"));
                    builder.depositAmount24h(FeatureCalculatorSupport.getDouble(rs, "deposit_amount_24h"));
                    builder.withdrawCount24h(FeatureCalculatorSupport.getInteger(rs, "withdraw_count_24h"));
                    builder.withdrawAmount24h(FeatureCalculatorSupport.getDouble(rs, "withdraw_amount_24h"));
                    builder.depositWithdrawRatio24h(FeatureCalculatorSupport.getDouble(rs, "deposit_withdraw_ratio_24h"));
                    builder.withdrawAfterDepositDelayAvg24h(FeatureCalculatorSupport.getDouble(rs, "withdraw_after_deposit_delay_avg_24h"));
                    builder.rapidWithdrawAfterDepositFlag24h(FeatureCalculatorSupport.getBoolean(rs, "rapid_withdraw_after_deposit_flag_24h"));
                }));
    }

    private void applyRewardMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with reward_deposits as (
                    select d.account_id,
                           count(*)::int as reward_transaction_count_30d,
                           max(d.created_at) as last_reward_deposit_time
                      from deposits d
                     where d.account_id in (:accountIds)
                       and d.source = 'reward'
                       and d.created_at > :windowStart
                       and d.created_at <= :generatedAt
                     group by d.account_id
                )
                select rd.account_id,
                       rd.reward_transaction_count_30d,
                       avg(extract(epoch from (w.created_at - rd.last_reward_deposit_time)) / 3600.0)::double precision
                           as reward_withdraw_delay_avg_30d
                  from reward_deposits rd
                  left join withdrawals w
                    on w.account_id = rd.account_id
                   and w.created_at >= rd.last_reward_deposit_time
                   and w.created_at <= :generatedAt
                 group by rd.account_id, rd.reward_transaction_count_30d
                """;
        MapSqlParameterSource params = FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_30D);
        jdbcTemplate.query(
                sql,
                params,
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.rewardTransactionCount30d(FeatureCalculatorSupport.getInteger(rs, "reward_transaction_count_30d"));
                    builder.rewardWithdrawDelayAvg30d(FeatureCalculatorSupport.getDouble(rs, "reward_withdraw_delay_avg_30d"));
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
