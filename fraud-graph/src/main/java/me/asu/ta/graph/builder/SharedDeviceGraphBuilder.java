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
public class SharedDeviceGraphBuilder {
    private static final int MAX_ACCOUNTS_PER_DEVICE = 20;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SharedDeviceGraphBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GraphEdge> buildEdges(Instant graphWindowStart, Instant graphWindowEnd) {
        Instant createdAt = Instant.now();
        return jdbcTemplate.query("""
                with bounded_devices as (
                    select device_id
                      from account_devices
                     where linked_at >= :graphWindowStart
                       and linked_at < :graphWindowEnd
                     group by device_id
                    having count(distinct account_id) between 2 and :maxAccountsPerDevice
                )
                select least(d1.account_id, d2.account_id) as from_account_id,
                       greatest(d1.account_id, d2.account_id) as to_account_id,
                       count(distinct d1.device_id) as shared_count,
                       min(d1.linked_at) as first_seen_at,
                       max(d1.linked_at) as last_seen_at
                  from account_devices d1
                  join account_devices d2
                    on d1.device_id = d2.device_id
                   and d1.account_id < d2.account_id
                  join bounded_devices bd
                    on bd.device_id = d1.device_id
                 where d1.linked_at >= :graphWindowStart
                   and d1.linked_at < :graphWindowEnd
                   and d2.linked_at >= :graphWindowStart
                   and d2.linked_at < :graphWindowEnd
                 group by least(d1.account_id, d2.account_id), greatest(d1.account_id, d2.account_id)
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd))
                .addValue("maxAccountsPerDevice", MAX_ACCOUNTS_PER_DEVICE), (rs, rowNum) -> new GraphEdge(
                0L,
                rs.getString("from_account_id"),
                rs.getString("to_account_id"),
                GraphEdgeType.SHARED_DEVICE,
                rs.getInt("shared_count"),
                rs.getInt("shared_count"),
                null,
                null,
                rs.getTimestamp("first_seen_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant(),
                graphWindowStart,
                graphWindowEnd,
                createdAt));
    }
}
