package me.asu.ta.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import me.asu.ta.ai.model.AiGenerationJob;
import me.asu.ta.ai.model.AiJobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiGenerationJobRepository {
    private static final RowMapper<AiGenerationJob> ROW_MAPPER = new AiGenerationJobRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AiGenerationJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiGenerationJob createJob(AiGenerationJob job) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into ai_generation_job(
                    job_type, started_at, finished_at, status, target_case_count,
                    processed_case_count, failed_case_count, error_message
                ) values (
                    :jobType, :startedAt, :finishedAt, :status, :targetCaseCount,
                    :processedCaseCount, :failedCaseCount, :errorMessage
                )
                """, new MapSqlParameterSource()
                .addValue("jobType", job.jobType())
                .addValue("startedAt", Timestamp.from(job.startedAt()))
                .addValue("finishedAt", job.finishedAt() == null ? null : Timestamp.from(job.finishedAt()))
                .addValue("status", job.status().name())
                .addValue("targetCaseCount", job.targetCaseCount())
                .addValue("processedCaseCount", job.processedCaseCount())
                .addValue("failedCaseCount", job.failedCaseCount())
                .addValue("errorMessage", job.errorMessage()), keyHolder, new String[]{"job_id"});
        Number key = keyHolder.getKey();
        return new AiGenerationJob(
                key == null ? null : key.longValue(),
                job.jobType(),
                job.startedAt(),
                job.finishedAt(),
                job.status(),
                job.targetCaseCount(),
                job.processedCaseCount(),
                job.failedCaseCount(),
                job.errorMessage());
    }

    public int updateJobStatus(long jobId, AiJobStatus status, java.time.Instant finishedAt, String errorMessage) {
        return jdbcTemplate.update("""
                update ai_generation_job
                   set status = :status,
                       finished_at = :finishedAt,
                       error_message = :errorMessage
                 where job_id = :jobId
                """, new MapSqlParameterSource()
                .addValue("jobId", jobId)
                .addValue("status", status.name())
                .addValue("finishedAt", finishedAt == null ? null : Timestamp.from(finishedAt))
                .addValue("errorMessage", errorMessage));
    }

    public int incrementProcessedCount(long jobId) {
        return jdbcTemplate.update("""
                update ai_generation_job
                   set processed_case_count = coalesce(processed_case_count, 0) + 1
                 where job_id = :jobId
                """, new MapSqlParameterSource("jobId", jobId));
    }

    public int incrementFailedCount(long jobId) {
        return jdbcTemplate.update("""
                update ai_generation_job
                   set failed_case_count = coalesce(failed_case_count, 0) + 1
                 where job_id = :jobId
                """, new MapSqlParameterSource("jobId", jobId));
    }

    public AiGenerationJob findById(long jobId) {
        return jdbcTemplate.queryForObject(
                "select * from ai_generation_job where job_id = :jobId",
                new MapSqlParameterSource("jobId", jobId),
                ROW_MAPPER);
    }

    private static final class AiGenerationJobRowMapper implements RowMapper<AiGenerationJob> {
        @Override
        public AiGenerationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp finishedAt = rs.getTimestamp("finished_at");
            return new AiGenerationJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    rs.getTimestamp("started_at").toInstant(),
                    finishedAt == null ? null : finishedAt.toInstant(),
                    AiJobStatus.valueOf(rs.getString("status")),
                    intOrNull(rs, "target_case_count"),
                    intOrNull(rs, "processed_case_count"),
                    intOrNull(rs, "failed_case_count"),
                    rs.getString("error_message"));
        }

        private Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }
    }
}
