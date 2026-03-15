package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseRiskSummaryRepository {
    private static final RowMapper<CaseRiskSummary> ROW_MAPPER = new CaseRiskSummaryRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseRiskSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(CaseRiskSummary summary) {
        return jdbcTemplate.update("""
                insert into case_risk_summary(
                    case_id, score_breakdown_json, rule_score, graph_score,
                    anomaly_score, behavior_score, created_at
                ) values (
                    :caseId, :scoreBreakdownJson, :ruleScore, :graphScore,
                    :anomalyScore, :behaviorScore, :createdAt
                )
                """, params(summary));
    }

    public Optional<CaseRiskSummary> findByCaseId(long caseId) {
        List<CaseRiskSummary> rows = jdbcTemplate.query(
                "select * from case_risk_summary where case_id = :caseId",
                new MapSqlParameterSource("caseId", caseId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(CaseRiskSummary summary) {
        return new MapSqlParameterSource()
                .addValue("caseId", summary.caseId())
                .addValue("scoreBreakdownJson", summary.scoreBreakdownJson())
                .addValue("ruleScore", summary.ruleScore())
                .addValue("graphScore", summary.graphScore())
                .addValue("anomalyScore", summary.anomalyScore())
                .addValue("behaviorScore", summary.behaviorScore())
                .addValue("createdAt", Timestamp.from(summary.createdAt()));
    }

    private static final class CaseRiskSummaryRowMapper implements RowMapper<CaseRiskSummary> {
        @Override
        public CaseRiskSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseRiskSummary(
                    rs.getLong("case_id"),
                    rs.getString("score_breakdown_json"),
                    doubleOrNull(rs, "rule_score"),
                    doubleOrNull(rs, "graph_score"),
                    doubleOrNull(rs, "anomaly_score"),
                    doubleOrNull(rs, "behavior_score"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
