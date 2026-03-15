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
public class SharedIpGraphBuilder {
    private static final int MAX_ACCOUNTS_PER_IP = 15;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SharedIpGraphBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GraphEdge> buildEdges(Instant graphWindowStart, Instant graphWindowEnd) {
        Instant createdAt = Instant.now();
        return jdbcTemplate.query("""
                with bounded_ips as (
                    select ip_address
                      from login_logs
                     where login_time >= :graphWindowStart
                       and login_time < :graphWindowEnd
                     group by ip_address
                    having count(distinct account_id) between 2 and :maxAccountsPerIp
                )
                select least(l1.account_id, l2.account_id) as from_account_id,
                       greatest(l1.account_id, l2.account_id) as to_account_id,
                       count(distinct l1.ip_address) as shared_count,
                       min(l1.login_time) as first_seen_at,
                       max(l1.login_time) as last_seen_at
                  from login_logs l1
                  join login_logs l2
                    on l1.ip_address = l2.ip_address
                   and l1.account_id < l2.account_id
                  join bounded_ips bi
                    on bi.ip_address = l1.ip_address
                 where l1.login_time >= :graphWindowStart
                   and l1.login_time < :graphWindowEnd
                   and l2.login_time >= :graphWindowStart
                   and l2.login_time < :graphWindowEnd
                   and coalesce(l1.ip_risk_level, 'LOW') <> 'LOW'
                 group by least(l1.account_id, l2.account_id), greatest(l1.account_id, l2.account_id)
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd))
                .addValue("maxAccountsPerIp", MAX_ACCOUNTS_PER_IP), (rs, rowNum) -> new GraphEdge(
                0L,
                rs.getString("from_account_id"),
                rs.getString("to_account_id"),
                GraphEdgeType.SHARED_IP,
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
