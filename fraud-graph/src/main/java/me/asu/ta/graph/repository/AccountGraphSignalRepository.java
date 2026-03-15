package me.asu.ta.graph.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.graph.model.AccountGraphSignal;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountGraphSignalRepository {
    private static final RowMapper<AccountGraphSignal> ROW_MAPPER = new AccountGraphSignalRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountGraphSignalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(AccountGraphSignal signal) {
        return jdbcTemplate.update("""
                insert into account_graph_signal(
                    account_id, graph_window_start, graph_window_end, graph_score, graph_cluster_size,
                    risk_neighbor_count, two_hop_risk_neighbor_count, shared_device_accounts,
                    shared_ip_accounts, shared_bank_accounts, collector_account_flag,
                    funnel_in_degree, funnel_out_degree, local_density_score, cluster_risk_score, generated_at
                ) values (
                    :accountId, :graphWindowStart, :graphWindowEnd, :graphScore, :graphClusterSize,
                    :riskNeighborCount, :twoHopRiskNeighborCount, :sharedDeviceAccounts,
                    :sharedIpAccounts, :sharedBankAccounts, :collectorAccountFlag,
                    :funnelInDegree, :funnelOutDegree, :localDensityScore, :clusterRiskScore, :generatedAt
                )
                on conflict (account_id) do update
                    set graph_window_start = excluded.graph_window_start,
                        graph_window_end = excluded.graph_window_end,
                        graph_score = excluded.graph_score,
                        graph_cluster_size = excluded.graph_cluster_size,
                        risk_neighbor_count = excluded.risk_neighbor_count,
                        two_hop_risk_neighbor_count = excluded.two_hop_risk_neighbor_count,
                        shared_device_accounts = excluded.shared_device_accounts,
                        shared_ip_accounts = excluded.shared_ip_accounts,
                        shared_bank_accounts = excluded.shared_bank_accounts,
                        collector_account_flag = excluded.collector_account_flag,
                        funnel_in_degree = excluded.funnel_in_degree,
                        funnel_out_degree = excluded.funnel_out_degree,
                        local_density_score = excluded.local_density_score,
                        cluster_risk_score = excluded.cluster_risk_score,
                        generated_at = excluded.generated_at
                """, params(signal));
    }

    public int batchInsert(List<AccountGraphSignal> signals) {
        int updated = 0;
        for (AccountGraphSignal signal : signals) {
            updated += save(signal);
        }
        return updated;
    }

    public Optional<AccountGraphSignal> findByAccountId(String accountId) {
        List<AccountGraphSignal> rows = jdbcTemplate.query(
                "select * from account_graph_signal where account_id = :accountId",
                new MapSqlParameterSource("accountId", accountId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int deleteByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.update("""
                delete from account_graph_signal
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)));
    }

    private MapSqlParameterSource params(AccountGraphSignal signal) {
        return new MapSqlParameterSource()
                .addValue("accountId", signal.accountId())
                .addValue("graphWindowStart", Timestamp.from(signal.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(signal.graphWindowEnd()))
                .addValue("graphScore", signal.graphScore())
                .addValue("graphClusterSize", signal.graphClusterSize())
                .addValue("riskNeighborCount", signal.riskNeighborCount())
                .addValue("twoHopRiskNeighborCount", signal.twoHopRiskNeighborCount())
                .addValue("sharedDeviceAccounts", signal.sharedDeviceAccounts())
                .addValue("sharedIpAccounts", signal.sharedIpAccounts())
                .addValue("sharedBankAccounts", signal.sharedBankAccounts())
                .addValue("collectorAccountFlag", signal.collectorAccountFlag())
                .addValue("funnelInDegree", signal.funnelInDegree())
                .addValue("funnelOutDegree", signal.funnelOutDegree())
                .addValue("localDensityScore", signal.localDensityScore())
                .addValue("clusterRiskScore", signal.clusterRiskScore())
                .addValue("generatedAt", Timestamp.from(signal.generatedAt()));
    }

    private static final class AccountGraphSignalRowMapper implements RowMapper<AccountGraphSignal> {
        @Override
        public AccountGraphSignal mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AccountGraphSignal(
                    rs.getString("account_id"),
                    rs.getTimestamp("graph_window_start").toInstant(),
                    rs.getTimestamp("graph_window_end").toInstant(),
                    rs.getDouble("graph_score"),
                    intOrNull(rs, "graph_cluster_size"),
                    intOrNull(rs, "risk_neighbor_count"),
                    intOrNull(rs, "two_hop_risk_neighbor_count"),
                    intOrNull(rs, "shared_device_accounts"),
                    intOrNull(rs, "shared_ip_accounts"),
                    intOrNull(rs, "shared_bank_accounts"),
                    boolOrNull(rs, "collector_account_flag"),
                    intOrNull(rs, "funnel_in_degree"),
                    intOrNull(rs, "funnel_out_degree"),
                    doubleOrNull(rs, "local_density_score"),
                    doubleOrNull(rs, "cluster_risk_score"),
                    rs.getTimestamp("generated_at").toInstant());
        }

        private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }

        private static Boolean boolOrNull(ResultSet rs, String column) throws SQLException {
            boolean value = rs.getBoolean(column);
            return rs.wasNull() ? null : value;
        }
    }
}
