package me.asu.ta.rule.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.rule.model.RuleEvaluationJob;
import me.asu.ta.rule.model.RuleJobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RuleEvaluationJobRepository {
    private static final RowMapper<RuleEvaluationJob> ROW_MAPPER = new RuleEvaluationJobRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RuleEvaluationJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createJob(RuleEvaluationJob job) {
        String sql = """
                insert into rule_evaluation_job(
                    job_type, started_at, finished_at, status,
                    target_account_count, processed_account_count, hit_account_count, failed_account_count, error_message
                ) values (
                    :jobType, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :hitAccountCount, :failedAccountCount, :errorMessage
                )
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params(job), keyHolder, new String[]{"job_id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated job_id");
        }
        return key.longValue();
    }

    public int save(RuleEvaluationJob job) {
        return jdbcTemplate.update(
                """
                insert into rule_evaluation_job(
                    job_id, job_type, started_at, finished_at, status,
                    target_account_count, processed_account_count, hit_account_count, failed_account_count, error_message
                ) values (
                    :jobId, :jobType, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :hitAccountCount, :failedAccountCount, :errorMessage
                )
                """,
                params(job));
    }

    public int update(RuleEvaluationJob job) {
        return updateJobStatus(job);
    }

    public int updateJobStatus(RuleEvaluationJob job) {
        String sql = """
                update rule_evaluation_job
                   set job_type = :jobType,
                       started_at = :startedAt,
                       finished_at = :finishedAt,
                       status = :status,
                       target_account_count = :targetAccountCount,
                       processed_account_count = :processedAccountCount,
                       hit_account_count = :hitAccountCount,
                       failed_account_count = :failedAccountCount,
                       error_message = :errorMessage
                 where job_id = :jobId
                """;
        return jdbcTemplate.update(sql, params(job));
    }

    public Optional<RuleEvaluationJob> findByRuleCode(String jobType) {
        String sql = """
                select * from rule_evaluation_job
                 where job_type = :jobType
                 order by started_at desc, job_id desc
                 limit 1
                """;
        List<RuleEvaluationJob> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("jobType", jobType), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(RuleEvaluationJob job) {
        return new MapSqlParameterSource()
                .addValue("jobId", job.jobId())
                .addValue("jobType", job.jobType())
                .addValue("startedAt", Timestamp.from(job.startedAt()))
                .addValue("finishedAt", toTimestamp(job.finishedAt()))
                .addValue("status", job.status().name())
                .addValue("targetAccountCount", job.targetAccountCount())
                .addValue("processedAccountCount", job.processedAccountCount())
                .addValue("hitAccountCount", job.hitAccountCount())
                .addValue("failedAccountCount", job.failedAccountCount())
                .addValue("errorMessage", job.errorMessage());
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static final class RuleEvaluationJobRowMapper implements RowMapper<RuleEvaluationJob> {
        @Override
        public RuleEvaluationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RuleEvaluationJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    toInstant(rs.getTimestamp("started_at")),
                    toInstant(rs.getTimestamp("finished_at")),
                    RuleJobStatus.valueOf(rs.getString("status")),
                    (Integer) rs.getObject("target_account_count"),
                    (Integer) rs.getObject("processed_account_count"),
                    (Integer) rs.getObject("hit_account_count"),
                    (Integer) rs.getObject("failed_account_count"),
                    rs.getString("error_message"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
