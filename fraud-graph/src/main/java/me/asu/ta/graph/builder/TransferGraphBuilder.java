package me.asu.ta.graph.builder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransferGraphBuilder {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TransferGraphBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GraphEdge> buildEdges(Instant graphWindowStart, Instant graphWindowEnd) {
        Instant createdAt = Instant.now();
        return jdbcTemplate.query("""
                select from_account_id,
                       to_account_id,
                       count(*) as transfer_count,
                       sum(amount) as transfer_amount_total,
                       min(created_at) as first_seen_at,
                       max(created_at) as last_seen_at
                 from transfers
                 where created_at >= :graphWindowStart
                   and created_at < :graphWindowEnd
                   and from_account_id <> to_account_id
                 group by from_account_id, to_account_id
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)), (rs, rowNum) -> new GraphEdge(
                0L,
                rs.getString("from_account_id"),
                rs.getString("to_account_id"),
                GraphEdgeType.TRANSFER,
                Math.min(rs.getInt("transfer_count") + rs.getDouble("transfer_amount_total") / 10000.0d, 100.0d),
                null,
                rs.getInt("transfer_count"),
                rs.getDouble("transfer_amount_total"),
                rs.getTimestamp("first_seen_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant(),
                graphWindowStart,
                graphWindowEnd,
                createdAt));
    }
}
