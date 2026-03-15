package me.asu.ta.casemanagement.repository;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseAccountBatchReader {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseAccountBatchReader(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countAccounts() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from account_feature_snapshot",
                new MapSqlParameterSource(),
                Integer.class);
        return count == null ? 0 : count;
    }

    public List<String> nextBatch(String lastAccountId, int batchSize) {
        return jdbcTemplate.query("""
                select account_id
                  from account_feature_snapshot
                 where (:lastAccountId is null or account_id > :lastAccountId)
                 order by account_id asc
                 limit :batchSize
                """, new MapSqlParameterSource()
                .addValue("lastAccountId", lastAccountId)
                .addValue("batchSize", batchSize), (rs, rowNum) -> rs.getString("account_id"));
    }
}
