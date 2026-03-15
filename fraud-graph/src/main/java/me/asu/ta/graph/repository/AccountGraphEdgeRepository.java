package me.asu.ta.graph.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountGraphEdgeRepository {
    private static final RowMapper<GraphEdge> ROW_MAPPER = new GraphEdgeRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountGraphEdgeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(GraphEdge edge) {
        return batchInsert(List.of(edge));
    }

    public int batchInsert(List<GraphEdge> edges) {
        if (edges.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.batchUpdate("""
                insert into account_graph_edge(
                    from_account_id, to_account_id, edge_type, edge_weight, shared_count,
                    transfer_count, transfer_amount_total, first_seen_at, last_seen_at,
                    graph_window_start, graph_window_end, created_at
                ) values (
                    :fromAccountId, :toAccountId, :edgeType, :edgeWeight, :sharedCount,
                    :transferCount, :transferAmountTotal, :firstSeenAt, :lastSeenAt,
                    :graphWindowStart, :graphWindowEnd, :createdAt
                )
                """, edges.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    public int deleteByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.update("""
                delete from account_graph_edge
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)));
    }

    public List<GraphEdge> findByAccountId(String accountId) {
        return jdbcTemplate.query("""
                select * from account_graph_edge
                 where from_account_id = :accountId or to_account_id = :accountId
                 order by edge_id asc
                """, new MapSqlParameterSource("accountId", accountId), ROW_MAPPER);
    }

    public List<GraphEdge> findByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.query("""
                select * from account_graph_edge
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                 order by edge_id asc
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)), ROW_MAPPER);
    }

    private MapSqlParameterSource params(GraphEdge edge) {
        return new MapSqlParameterSource()
                .addValue("fromAccountId", edge.fromAccountId())
                .addValue("toAccountId", edge.toAccountId())
                .addValue("edgeType", edge.edgeType().name())
                .addValue("edgeWeight", edge.edgeWeight())
                .addValue("sharedCount", edge.sharedCount())
                .addValue("transferCount", edge.transferCount())
                .addValue("transferAmountTotal", edge.transferAmountTotal())
                .addValue("firstSeenAt", toTimestamp(edge.firstSeenAt()))
                .addValue("lastSeenAt", toTimestamp(edge.lastSeenAt()))
                .addValue("graphWindowStart", Timestamp.from(edge.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(edge.graphWindowEnd()))
                .addValue("createdAt", Timestamp.from(edge.createdAt()));
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static final class GraphEdgeRowMapper implements RowMapper<GraphEdge> {
        @Override
        public GraphEdge mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GraphEdge(
                    rs.getLong("edge_id"),
                    rs.getString("from_account_id"),
                    rs.getString("to_account_id"),
                    GraphEdgeType.valueOf(rs.getString("edge_type")),
                    rs.getDouble("edge_weight"),
                    intOrNull(rs, "shared_count"),
                    intOrNull(rs, "transfer_count"),
                    doubleOrNull(rs, "transfer_amount_total"),
                    toInstant(rs.getTimestamp("first_seen_at")),
                    toInstant(rs.getTimestamp("last_seen_at")),
                    rs.getTimestamp("graph_window_start").toInstant(),
                    rs.getTimestamp("graph_window_end").toInstant(),
                    rs.getTimestamp("created_at").toInstant());
        }

        private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
