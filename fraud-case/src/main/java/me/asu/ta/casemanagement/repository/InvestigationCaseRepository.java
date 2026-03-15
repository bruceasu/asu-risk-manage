package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.rule.model.EvaluationMode;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class InvestigationCaseRepository {
    private static final RowMapper<InvestigationCase> ROW_MAPPER = new InvestigationCaseRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public InvestigationCaseRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InvestigationCase save(InvestigationCase investigationCase) {
        String sql = """
                insert into investigation_case(
                    account_id, case_status, risk_score, risk_level, profile_name,
                    top_reason_codes, feature_version, evaluation_mode, created_at, updated_at
                ) values (
                    :accountId, :caseStatus, :riskScore, :riskLevel, :profileName,
                    :topReasonCodes, :featureVersion, :evaluationMode, :createdAt, :updatedAt
                )
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params(investigationCase), keyHolder, new String[]{"case_id"});
        Number key = keyHolder.getKey();
        return new InvestigationCase(
                key == null ? 0L : key.longValue(),
                investigationCase.accountId(),
                investigationCase.caseStatus(),
                investigationCase.riskScore(),
                investigationCase.riskLevel(),
                investigationCase.profileName(),
                investigationCase.topReasonCodes(),
                investigationCase.featureVersion(),
                investigationCase.evaluationMode(),
                investigationCase.createdAt(),
                investigationCase.updatedAt());
    }

    public Optional<InvestigationCase> findByCaseId(long caseId) {
        List<InvestigationCase> rows = jdbcTemplate.query(
                "select * from investigation_case where case_id = :caseId",
                new MapSqlParameterSource("caseId", caseId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<InvestigationCase> findLatestByAccountId(String accountId) {
        List<InvestigationCase> rows = jdbcTemplate.query("""
                select * from investigation_case
                 where account_id = :accountId
                 order by created_at desc, case_id desc
                 limit 1
                """, new MapSqlParameterSource("accountId", accountId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<InvestigationCase> findBatchByAccountIds(List<String> accountIds) {
        return jdbcTemplate.query("""
                select * from investigation_case
                 where account_id in (:accountIds)
                 order by created_at desc, case_id desc
                """, new MapSqlParameterSource("accountIds", accountIds), ROW_MAPPER);
    }

    private MapSqlParameterSource params(InvestigationCase investigationCase) {
        return new MapSqlParameterSource()
                .addValue("accountId", investigationCase.accountId())
                .addValue("caseStatus", investigationCase.caseStatus().name())
                .addValue("riskScore", investigationCase.riskScore())
                .addValue("riskLevel", investigationCase.riskLevel().name())
                .addValue("profileName", investigationCase.profileName())
                .addValue("topReasonCodes", String.join(",", investigationCase.topReasonCodes()))
                .addValue("featureVersion", investigationCase.featureVersion())
                .addValue("evaluationMode", investigationCase.evaluationMode().name())
                .addValue("createdAt", Timestamp.from(investigationCase.createdAt()))
                .addValue("updatedAt", Timestamp.from(investigationCase.updatedAt()));
    }

    private static final class InvestigationCaseRowMapper implements RowMapper<InvestigationCase> {
        @Override
        public InvestigationCase mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InvestigationCase(
                    rs.getLong("case_id"),
                    rs.getString("account_id"),
                    CaseStatus.valueOf(rs.getString("case_status")),
                    rs.getDouble("risk_score"),
                    RiskLevel.valueOf(rs.getString("risk_level")),
                    rs.getString("profile_name"),
                    parseReasonCodes(rs.getString("top_reason_codes")),
                    rs.getInt("feature_version"),
                    EvaluationMode.valueOf(rs.getString("evaluation_mode")),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private static List<String> parseReasonCodes(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isBlank()).collect(Collectors.toList());
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
