package me.asu.ta.feature.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.FeatureGenerationJob;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class FeatureGenerationJobRepository {
    private static final RowMapper<FeatureGenerationJob> ROW_MAPPER = new FeatureGenerationJobRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FeatureGenerationJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(FeatureGenerationJob job) {
        String sql = """
                insert into feature_generation_job (
                    job_type, feature_version, started_at, finished_at, status,
                    target_account_count, processed_account_count, failed_account_count, error_message
                ) values (
                    :jobType, :featureVersion, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :failedAccountCount, :errorMessage
                )
                """;
        return jdbcTemplate.update(sql, params(job));
    }

    public long saveAndReturnId(FeatureGenerationJob job) {
        String sql = """
                insert into feature_generation_job (
                    job_type, feature_version, started_at, finished_at, status,
                    target_account_count, processed_account_count, failed_account_count, error_message
                ) values (
                    :jobType, :featureVersion, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :failedAccountCount, :errorMessage
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

    public int update(FeatureGenerationJob job) {
        String sql = """
                update feature_generation_job
                   set job_type = :jobType,
                       feature_version = :featureVersion,
                       started_at = :startedAt,
                       finished_at = :finishedAt,
                       status = :status,
                       target_account_count = :targetAccountCount,
                       processed_account_count = :processedAccountCount,
                       failed_account_count = :failedAccountCount,
                       error_message = :errorMessage
                 where job_id = :jobId
                """;
        return jdbcTemplate.update(sql, params(job));
    }

    public Optional<FeatureGenerationJob> findById(long jobId) {
        String sql = "select * from feature_generation_job where job_id = :jobId";
        List<FeatureGenerationJob> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("jobId", jobId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<FeatureGenerationJob> findBatch(List<Long> jobIds) {
        String sql = "select * from feature_generation_job where job_id in (:jobIds) order by job_id";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("jobIds", jobIds), ROW_MAPPER);
    }

    public Optional<FeatureGenerationJob> findLatestByAccountId(String accountId) {
        return Optional.empty();
    }

    public int insertBatch(List<FeatureGenerationJob> jobs) {
        String sql = """
                insert into feature_generation_job (
                    job_type, feature_version, started_at, finished_at, status,
                    target_account_count, processed_account_count, failed_account_count, error_message
                ) values (
                    :jobType, :featureVersion, :startedAt, :finishedAt, :status,
                    :targetAccountCount, :processedAccountCount, :failedAccountCount, :errorMessage
                )
                """;
        return jdbcTemplate.batchUpdate(sql, jobs.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(FeatureGenerationJob job) {
        return new MapSqlParameterSource()
                .addValue("jobId", job.jobId())
                .addValue("jobType", job.jobType())
                .addValue("featureVersion", job.featureVersion())
                .addValue("startedAt", Timestamp.from(job.startedAt()))
                .addValue("finishedAt", toTimestamp(job.finishedAt()))
                .addValue("status", job.status())
                .addValue("targetAccountCount", job.targetAccountCount())
                .addValue("processedAccountCount", job.processedAccountCount())
                .addValue("failedAccountCount", job.failedAccountCount())
                .addValue("errorMessage", job.errorMessage());
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static final class FeatureGenerationJobRowMapper implements RowMapper<FeatureGenerationJob> {
        @Override
        public FeatureGenerationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FeatureGenerationJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    rs.getInt("feature_version"),
                    toInstant(rs.getTimestamp("started_at")),
                    toInstant(rs.getTimestamp("finished_at")),
                    rs.getString("status"),
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
