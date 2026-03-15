package me.asu.ta.graph.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.graph.model.GraphClusterType;
import me.asu.ta.graph.model.GraphRiskSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GraphRiskSummaryRepository {
    private static final RowMapper<GraphRiskSummary> ROW_MAPPER = new GraphRiskSummaryRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GraphRiskSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(GraphRiskSummary summary) {
        return jdbcTemplate.update("""
                insert into graph_risk_summary(
                    cluster_id, cluster_type, cluster_size, high_risk_node_count,
                    shared_device_edge_count, shared_ip_edge_count, shared_bank_edge_count,
                    transfer_edge_count, collector_present_flag, cluster_risk_score,
                    graph_window_start, graph_window_end, generated_at
                ) values (
                    :clusterId, :clusterType, :clusterSize, :highRiskNodeCount,
                    :sharedDeviceEdgeCount, :sharedIpEdgeCount, :sharedBankEdgeCount,
                    :transferEdgeCount, :collectorPresentFlag, :clusterRiskScore,
                    :graphWindowStart, :graphWindowEnd, :generatedAt
                )
                on conflict (cluster_id) do update
                    set cluster_type = excluded.cluster_type,
                        cluster_size = excluded.cluster_size,
                        high_risk_node_count = excluded.high_risk_node_count,
                        shared_device_edge_count = excluded.shared_device_edge_count,
                        shared_ip_edge_count = excluded.shared_ip_edge_count,
                        shared_bank_edge_count = excluded.shared_bank_edge_count,
                        transfer_edge_count = excluded.transfer_edge_count,
                        collector_present_flag = excluded.collector_present_flag,
                        cluster_risk_score = excluded.cluster_risk_score,
                        graph_window_start = excluded.graph_window_start,
                        graph_window_end = excluded.graph_window_end,
                        generated_at = excluded.generated_at
                """, params(summary));
    }

    public int batchInsert(List<GraphRiskSummary> summaries) {
        int updated = 0;
        for (GraphRiskSummary summary : summaries) {
            updated += save(summary);
        }
        return updated;
    }

    public List<GraphRiskSummary> findByAccountId(String accountId) {
        return jdbcTemplate.query("""
                select s.*
                  from graph_risk_summary s
                  join account_graph_cluster c
                    on c.cluster_id = s.cluster_id
                 where c.account_id = :accountId
                 order by s.generated_at desc, s.cluster_id asc
                """, new MapSqlParameterSource("accountId", accountId), ROW_MAPPER);
    }

    public Optional<GraphRiskSummary> findByClusterId(String clusterId) {
        List<GraphRiskSummary> rows = jdbcTemplate.query(
                "select * from graph_risk_summary where cluster_id = :clusterId",
                new MapSqlParameterSource("clusterId", clusterId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int deleteByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.update("""
                delete from graph_risk_summary
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)));
    }

    private MapSqlParameterSource params(GraphRiskSummary summary) {
        return new MapSqlParameterSource()
                .addValue("clusterId", summary.clusterId())
                .addValue("clusterType", summary.clusterType().name())
                .addValue("clusterSize", summary.clusterSize())
                .addValue("highRiskNodeCount", summary.highRiskNodeCount())
                .addValue("sharedDeviceEdgeCount", summary.sharedDeviceEdgeCount())
                .addValue("sharedIpEdgeCount", summary.sharedIpEdgeCount())
                .addValue("sharedBankEdgeCount", summary.sharedBankEdgeCount())
                .addValue("transferEdgeCount", summary.transferEdgeCount())
                .addValue("collectorPresentFlag", summary.collectorPresentFlag())
                .addValue("clusterRiskScore", summary.clusterRiskScore())
                .addValue("graphWindowStart", Timestamp.from(summary.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(summary.graphWindowEnd()))
                .addValue("generatedAt", Timestamp.from(summary.generatedAt()));
    }

    private static final class GraphRiskSummaryRowMapper implements RowMapper<GraphRiskSummary> {
        @Override
        public GraphRiskSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GraphRiskSummary(
                    rs.getString("cluster_id"),
                    GraphClusterType.valueOf(rs.getString("cluster_type")),
                    rs.getInt("cluster_size"),
                    rs.getInt("high_risk_node_count"),
                    intOrNull(rs, "shared_device_edge_count"),
                    intOrNull(rs, "shared_ip_edge_count"),
                    intOrNull(rs, "shared_bank_edge_count"),
                    intOrNull(rs, "transfer_edge_count"),
                    boolOrNull(rs, "collector_present_flag"),
                    doubleOrNull(rs, "cluster_risk_score"),
                    rs.getTimestamp("graph_window_start").toInstant(),
                    rs.getTimestamp("graph_window_end").toInstant(),
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
