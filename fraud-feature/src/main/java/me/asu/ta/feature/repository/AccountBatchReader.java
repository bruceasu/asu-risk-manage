package me.asu.ta.feature.repository;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountBatchReader {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountBatchReader(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countAccounts() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from accounts", new MapSqlParameterSource(), Integer.class);
        return count == null ? 0 : count;
    }

    public List<String> fetchNextBatch(String lastAccountId, int batchSize) {
        String sql = """
                select account_id
                  from accounts
                 where (:lastAccountId is null or account_id > :lastAccountId)
                 order by account_id
                 limit :batchSize
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("lastAccountId", lastAccountId)
                        .addValue("batchSize", batchSize),
                (rs, rowNum) -> rs.getString("account_id"));
    }
}
