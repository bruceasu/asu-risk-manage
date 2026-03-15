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
public class SharedBankGraphBuilder {
    private static final int MAX_ACCOUNTS_PER_BANK_ACCOUNT = 10;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SharedBankGraphBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GraphEdge> buildEdges(Instant graphWindowStart, Instant graphWindowEnd) {
        Instant createdAt = Instant.now();
        return jdbcTemplate.query("""
                with bounded_bank_accounts as (
                    select bank_account_id
                      from bank_accounts
                     where linked_at >= :graphWindowStart
                       and linked_at < :graphWindowEnd
                     group by bank_account_id
                    having count(distinct account_id) between 2 and :maxAccountsPerBankAccount
                )
                select least(b1.account_id, b2.account_id) as from_account_id,
                       greatest(b1.account_id, b2.account_id) as to_account_id,
                       count(distinct b1.bank_account_id) as shared_count,
                       min(b1.linked_at) as first_seen_at,
                       max(b1.linked_at) as last_seen_at
                  from bank_accounts b1
                  join bank_accounts b2
                    on b1.bank_account_id = b2.bank_account_id
                   and b1.account_id < b2.account_id
                  join bounded_bank_accounts bb
                    on bb.bank_account_id = b1.bank_account_id
                 where b1.linked_at >= :graphWindowStart
                   and b1.linked_at < :graphWindowEnd
                   and b2.linked_at >= :graphWindowStart
                   and b2.linked_at < :graphWindowEnd
                 group by least(b1.account_id, b2.account_id), greatest(b1.account_id, b2.account_id)
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd))
                .addValue("maxAccountsPerBankAccount", MAX_ACCOUNTS_PER_BANK_ACCOUNT), (rs, rowNum) -> new GraphEdge(
                0L,
                rs.getString("from_account_id"),
                rs.getString("to_account_id"),
                GraphEdgeType.SHARED_BANK_ACCOUNT,
                rs.getInt("shared_count") * 2.0d,
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
