package me.asu.ta.risk.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.rule.model.EvaluationMode;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RiskScoreResultRepository {
    private final RowMapper<RiskScoreResult> rowMapper = new RiskScoreResultRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RiskScoreResultRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public RiskScoreResult saveRiskScoreResult(RiskScoreResult result) {
        String sql = """
                insert into risk_score_result(
                    account_id, risk_score, risk_level, profile_name, feature_version,
                    generated_at, evaluation_mode, top_reason_codes, score_breakdown_json
                ) values (
                    :accountId, :riskScore, :riskLevel, :profileName, :featureVersion,
                    :generatedAt, :evaluationMode, :topReasonCodes, :scoreBreakdownJson
                )
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = params(result);
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"score_id"});
        Number key = keyHolder.getKey();
        return new RiskScoreResult(
                key == null ? 0L : key.longValue(),
                result.accountId(),
                result.riskScore(),
                result.riskLevel(),
                result.profileName(),
                result.featureVersion(),
                result.generatedAt(),
                result.evaluationMode(),
                result.topReasonCodes(),
                result.scoreBreakdown());
    }

    public Optional<RiskScoreResult> findLatestRiskScoreByAccountId(String accountId) {
        String sql = """
                select * from risk_score_result
                 where account_id = :accountId
                 order by generated_at desc, score_id desc
                 limit 1
                """;
        List<RiskScoreResult> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("accountId", accountId), rowMapper);
        return rows.stream().findFirst();
    }

    public List<RiskScoreResult> findHistoryByAccountId(String accountId, int limit, int offset) {
        String sql = """
                select * from risk_score_result
                 where account_id = :accountId
                 order by generated_at desc, score_id desc
                 limit :limit
                offset :offset
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                rowMapper);
    }

    public int insertBatch(List<RiskScoreResult> results) {
        String sql = """
                insert into risk_score_result(
                    account_id, risk_score, risk_level, profile_name, feature_version,
                    generated_at, evaluation_mode, top_reason_codes, score_breakdown_json
                ) values (
                    :accountId, :riskScore, :riskLevel, :profileName, :featureVersion,
                    :generatedAt, :evaluationMode, :topReasonCodes, :scoreBreakdownJson
                )
                """;
        return jdbcTemplate.batchUpdate(sql, results.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(RiskScoreResult result) {
        return new MapSqlParameterSource()
                .addValue("accountId", result.accountId())
                .addValue("riskScore", result.riskScore())
                .addValue("riskLevel", result.riskLevel().name())
                .addValue("profileName", result.profileName())
                .addValue("featureVersion", result.featureVersion())
                .addValue("generatedAt", Timestamp.from(result.generatedAt()))
                .addValue("evaluationMode", result.evaluationMode().name())
                .addValue("topReasonCodes", String.join(",", result.topReasonCodes()))
                .addValue("scoreBreakdownJson", toJson(result.scoreBreakdown()));
    }

    private String toJson(ScoreBreakdown breakdown) {
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize score breakdown", ex);
        }
    }

    private final class RiskScoreResultRowMapper implements RowMapper<RiskScoreResult> {
        @Override
        public RiskScoreResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskScoreResult(
                    rs.getLong("score_id"),
                    rs.getString("account_id"),
                    rs.getDouble("risk_score"),
                    RiskLevel.valueOf(rs.getString("risk_level")),
                    rs.getString("profile_name"),
                    rs.getInt("feature_version"),
                    toInstant(rs.getTimestamp("generated_at")),
                    EvaluationMode.valueOf(rs.getString("evaluation_mode")),
                    parseReasonCodes(rs.getString("top_reason_codes")),
                    parseBreakdown(rs.getString("score_breakdown_json")));
        }

        private List<String> parseReasonCodes(String topReasonCodes) {
            if (topReasonCodes == null || topReasonCodes.isBlank()) {
                return List.of();
            }
            return Arrays.stream(topReasonCodes.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toList());
        }

        private ScoreBreakdown parseBreakdown(String json) {
            try {
                return objectMapper.readValue(json, ScoreBreakdown.class);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to deserialize score breakdown", ex);
            }
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
