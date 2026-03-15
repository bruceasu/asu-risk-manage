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
public class GraphFeatureCalculator implements FeatureCalculator {
    private static final Duration WINDOW_7D = Duration.ofDays(7);
    private static final Duration WINDOW_30D = Duration.ofDays(30);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GraphFeatureCalculator(NamedParameterJdbcTemplate jdbcTemplate) {
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
        applySharedIpMetrics(accountIds, generatedAt, builders);
        applySharedBankAndClusterMetrics(accountIds, generatedAt, builders);
        applyRiskNeighborMetrics(accountIds, generatedAt, builders);
    }

    private void applySharedIpMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with recent_ips as (
                    select distinct ll.account_id, ll.ip
                      from login_logs ll
                     where ll.account_id in (:accountIds)
                       and ll.login_time > :windowStart
                       and ll.login_time <= :generatedAt
                )
                select rip.account_id,
                       sum(shared.shared_ip_accounts)::int as shared_ip_accounts_7d
                  from recent_ips rip
                  join (
                        select ll.ip,
                               greatest(count(distinct ll.account_id) - 1, 0)::int as shared_ip_accounts
                          from login_logs ll
                         where ll.login_time > :windowStart
                           and ll.login_time <= :generatedAt
                         group by ll.ip
                  ) shared on shared.ip = rip.ip
                 group by rip.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_7D),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.sharedIpAccounts7d(FeatureCalculatorSupport.getInteger(rs, "shared_ip_accounts_7d"));
                }));
    }

    private void applySharedBankAndClusterMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                with recent_bank_accounts as (
                    select ba.account_id,
                           ba.bank_account_id
                      from bank_accounts ba
                     where ba.account_id in (:accountIds)
                ),
                shared_bank as (
                    select rba.account_id,
                           sum(shared.shared_bank_accounts)::int as shared_bank_accounts_30d
                      from recent_bank_accounts rba
                      join (
                            select ba.bank_account_id,
                                   greatest(count(distinct ba.account_id) - 1, 0)::int as shared_bank_accounts
                              from bank_accounts ba
                             group by ba.bank_account_id
                      ) shared on shared.bank_account_id = rba.bank_account_id
                     group by rba.account_id
                ),
                cluster_sizes as (
                    select ac.account_id,
                           count(*) over (partition by ac.cluster_id)::int as graph_cluster_size_30d
                      from account_clusters ac
                     where ac.account_id in (:accountIds)
                )
                select coalesce(sb.account_id, cs.account_id) as account_id,
                       sb.shared_bank_accounts_30d,
                       cs.graph_cluster_size_30d
                  from shared_bank sb
                  full outer join cluster_sizes cs on cs.account_id = sb.account_id
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_30D),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.sharedBankAccounts30d(FeatureCalculatorSupport.getInteger(rs, "shared_bank_accounts_30d"));
                    builder.graphClusterSize30d(FeatureCalculatorSupport.getInteger(rs, "graph_cluster_size_30d"));
                }));
    }

    private void applyRiskNeighborMetrics(List<String> accountIds, Instant generatedAt, Map<String, AccountFeatureSnapshot.Builder> builders) {
        String sql = """
                select age.from_account as account_id,
                       count(distinct fl.account_id)::int as risk_neighbor_count_30d,
                       avg(ic.risk_score)::double precision as anomaly_score_last
                  from account_graph_edges age
                  left join fraud_labels fl
                    on fl.account_id = age.to_account
                   and fl.labeled_at > :windowStart
                   and fl.labeled_at <= :generatedAt
                  left join investigation_cases ic
                    on ic.account_id = age.from_account
                 where age.from_account in (:accountIds)
                   and age.created_at > :windowStart
                   and age.created_at <= :generatedAt
                 group by age.from_account
                """;
        jdbcTemplate.query(
                sql,
                FeatureCalculatorSupport.batchParams(accountIds, generatedAt, WINDOW_30D),
                callback(rs -> {
                    AccountFeatureSnapshot.Builder builder = builders.get(rs.getString("account_id"));
                    if (builder == null) {
                        return;
                    }
                    builder.riskNeighborCount30d(FeatureCalculatorSupport.getInteger(rs, "risk_neighbor_count_30d"));
                    builder.anomalyScoreLast(FeatureCalculatorSupport.getDouble(rs, "anomaly_score_last"));
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
