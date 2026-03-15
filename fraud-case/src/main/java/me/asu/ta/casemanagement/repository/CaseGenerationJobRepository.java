package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseGenerationJob;
import me.asu.ta.casemanagement.model.CaseJobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class CaseGenerationJobRepository {
    private static final RowMapper<CaseGenerationJob> ROW_MAPPER = new CaseGenerationJobRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseGenerationJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CaseGenerationJob createJob(CaseGenerationJob job) {
        String sql = """
                insert into case_generation_job(
                    job_type, started_at, finished_at, status,
                    target_account_count, processed_account_count, failed_account_count, error_message
                ) values (
                    :jobType, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :failedAccountCount, :errorMessage
                )
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params(job), keyHolder, new String[]{"job_id"});
        Number key = keyHolder.getKey();
        return new CaseGenerationJob(
                key == null ? 0L : key.longValue(),
                job.jobType(),
                job.startedAt(),
                job.finishedAt(),
                job.status(),
                job.targetAccountCount(),
                job.processedAccountCount(),
                job.failedAccountCount(),
                job.errorMessage());
    }

    public int updateJobStatus(CaseGenerationJob job) {
        return jdbcTemplate.update("""
                update case_generation_job
                   set finished_at = :finishedAt,
                       status = :status,
                       target_account_count = :targetAccountCount,
                       processed_account_count = :processedAccountCount,
                       failed_account_count = :failedAccountCount,
                       error_message = :errorMessage
                 where job_id = :jobId
                """, params(job).addValue("jobId", job.jobId()));
    }

    public Optional<CaseGenerationJob> findById(long jobId) {
        List<CaseGenerationJob> rows = jdbcTemplate.query(
                "select * from case_generation_job where job_id = :jobId",
                new MapSqlParameterSource("jobId", jobId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(CaseGenerationJob job) {
        return new MapSqlParameterSource()
                .addValue("jobType", job.jobType())
                .addValue("startedAt", Timestamp.from(job.startedAt()))
                .addValue("finishedAt", toTimestamp(job.finishedAt()))
                .addValue("status", job.status().name())
                .addValue("targetAccountCount", job.targetAccountCount())
                .addValue("processedAccountCount", job.processedAccountCount())
                .addValue("failedAccountCount", job.failedAccountCount())
                .addValue("errorMessage", job.errorMessage());
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static final class CaseGenerationJobRowMapper implements RowMapper<CaseGenerationJob> {
        @Override
        public CaseGenerationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseGenerationJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    toInstant(rs.getTimestamp("started_at")),
                    toInstant(rs.getTimestamp("finished_at")),
                    CaseJobStatus.valueOf(rs.getString("status")),
                    intOrNull(rs, "target_account_count"),
                    intOrNull(rs, "processed_account_count"),
                    intOrNull(rs, "failed_account_count"),
                    rs.getString("error_message"));
        }

        private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
