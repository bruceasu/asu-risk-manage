package me.asu.ta.ai.repository;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvestigationCaseBatchReader {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public InvestigationCaseBatchReader(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countCases() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from investigation_case",
                new MapSqlParameterSource(),
                Integer.class);
        return count == null ? 0 : count;
    }

    public List<Long> nextBatch(Long lastCaseId, int batchSize) {
        return jdbcTemplate.query("""
                select case_id
                  from investigation_case
                 where (:lastCaseId is null or case_id > :lastCaseId)
                 order by case_id asc
                 limit :batchSize
                """, new MapSqlParameterSource()
                .addValue("lastCaseId", lastCaseId)
                .addValue("batchSize", batchSize), (rs, rowNum) -> rs.getLong("case_id"));
    }
}
