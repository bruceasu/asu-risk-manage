package me.asu.ta.feature.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.FeatureQualityCheck;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeatureQualityCheckRepository {
    private static final RowMapper<FeatureQualityCheck> ROW_MAPPER = new FeatureQualityCheckRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FeatureQualityCheckRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(FeatureQualityCheck check) {
        String sql = """
                insert into feature_quality_check (
                    check_time, feature_version, feature_name, check_type, status, total_records, failed_records, details
                ) values (
                    :checkTime, :featureVersion, :featureName, :checkType, :status, :totalRecords, :failedRecords, :details
                )
                """;
        return jdbcTemplate.update(sql, params(check));
    }

    public int update(FeatureQualityCheck check) {
        String sql = """
                update feature_quality_check
                   set check_time = :checkTime,
                       feature_version = :featureVersion,
                       feature_name = :featureName,
                       check_type = :checkType,
                       status = :status,
                       total_records = :totalRecords,
                       failed_records = :failedRecords,
                       details = :details
                 where check_id = :checkId
                """;
        return jdbcTemplate.update(sql, params(check));
    }

    public Optional<FeatureQualityCheck> findById(long checkId) {
        String sql = "select * from feature_quality_check where check_id = :checkId";
        List<FeatureQualityCheck> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("checkId", checkId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<FeatureQualityCheck> findBatch(List<Long> checkIds) {
        String sql = "select * from feature_quality_check where check_id in (:checkIds) order by check_id";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("checkIds", checkIds), ROW_MAPPER);
    }

    public Optional<FeatureQualityCheck> findLatestByAccountId(String accountId) {
        return Optional.empty();
    }

    public int insertBatch(List<FeatureQualityCheck> checks) {
        String sql = """
                insert into feature_quality_check (
                    check_time, feature_version, feature_name, check_type, status, total_records, failed_records, details
                ) values (
                    :checkTime, :featureVersion, :featureName, :checkType, :status, :totalRecords, :failedRecords, :details
                )
                """;
        return jdbcTemplate.batchUpdate(sql, checks.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(FeatureQualityCheck check) {
        return new MapSqlParameterSource()
                .addValue("checkId", check.checkId())
                .addValue("checkTime", Timestamp.from(check.checkTime()))
                .addValue("featureVersion", check.featureVersion())
                .addValue("featureName", check.featureName())
                .addValue("checkType", check.checkType())
                .addValue("status", check.status())
                .addValue("totalRecords", check.totalRecords())
                .addValue("failedRecords", check.failedRecords())
                .addValue("details", check.details());
    }

    private static final class FeatureQualityCheckRowMapper implements RowMapper<FeatureQualityCheck> {
        @Override
        public FeatureQualityCheck mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FeatureQualityCheck(
                    rs.getLong("check_id"),
                    toInstant(rs.getTimestamp("check_time")),
                    rs.getInt("feature_version"),
                    rs.getString("feature_name"),
                    rs.getString("check_type"),
                    rs.getString("status"),
                    (Integer) rs.getObject("total_records"),
                    (Integer) rs.getObject("failed_records"),
                    rs.getString("details"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
