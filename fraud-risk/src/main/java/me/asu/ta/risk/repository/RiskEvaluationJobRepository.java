package me.asu.ta.risk.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.risk.model.RiskEvaluationJob;
import me.asu.ta.risk.model.RiskJobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RiskEvaluationJobRepository {
    private static final RowMapper<RiskEvaluationJob> ROW_MAPPER = new RiskEvaluationJobRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskEvaluationJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RiskEvaluationJob createJob(RiskEvaluationJob job) {
        String sql = """
                insert into risk_evaluation_job(
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
        return new RiskEvaluationJob(
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

    public int updateJobStatus(RiskEvaluationJob job) {
        String sql = """
                update risk_evaluation_job
                   set finished_at = :finishedAt,
                       status = :status,
                       target_account_count = :targetAccountCount,
                       processed_account_count = :processedAccountCount,
                       failed_account_count = :failedAccountCount,
                       error_message = :errorMessage
                 where job_id = :jobId
                """;
        return jdbcTemplate.update(sql, params(job).addValue("jobId", job.jobId()));
    }

    public Optional<RiskEvaluationJob> findById(long jobId) {
        String sql = "select * from risk_evaluation_job where job_id = :jobId";
        List<RiskEvaluationJob> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("jobId", jobId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(RiskEvaluationJob job) {
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

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static final class RiskEvaluationJobRowMapper implements RowMapper<RiskEvaluationJob> {
        @Override
        public RiskEvaluationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskEvaluationJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    toInstant(rs.getTimestamp("started_at")),
                    toInstant(rs.getTimestamp("finished_at")),
                    RiskJobStatus.valueOf(rs.getString("status")),
                    (Integer) rs.getObject("target_account_count"),
                    (Integer) rs.getObject("processed_account_count"),
                    (Integer) rs.getObject("failed_account_count"),
                    rs.getString("error_message"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
