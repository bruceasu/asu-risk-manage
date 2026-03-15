package me.asu.ta.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.ai.model.InvestigationReportStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class InvestigationReportRepository {
    private static final RowMapper<InvestigationReport> ROW_MAPPER = new InvestigationReportRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public InvestigationReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InvestigationReport save(InvestigationReport report) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into investigation_report(
                    case_id, report_status, report_title, executive_summary, key_risk_indicators,
                    behavior_analysis, relationship_analysis, timeline_observations,
                    possible_risk_patterns, recommendations, model_name, template_code,
                    template_version, generated_at, raw_response
                ) values (
                    :caseId, :reportStatus, :reportTitle, :executiveSummary, :keyRiskIndicators,
                    :behaviorAnalysis, :relationshipAnalysis, :timelineObservations,
                    :possibleRiskPatterns, :recommendations, :modelName, :templateCode,
                    :templateVersion, :generatedAt, :rawResponse
                )
                """, params(report), keyHolder, new String[]{"report_id"});
        Number key = keyHolder.getKey();
        return new InvestigationReport(
                key == null ? null : key.longValue(),
                report.caseId(),
                report.reportStatus(),
                report.reportTitle(),
                report.executiveSummary(),
                report.keyRiskIndicators(),
                report.behaviorAnalysis(),
                report.relationshipAnalysis(),
                report.timelineObservations(),
                report.possibleRiskPatterns(),
                report.recommendations(),
                report.modelName(),
                report.templateCode(),
                report.templateVersion(),
                report.generatedAt(),
                report.rawResponse());
    }

    public List<InvestigationReport> findByCaseId(long caseId) {
        return jdbcTemplate.query("""
                select *
                  from investigation_report
                 where case_id = :caseId
                 order by generated_at desc, report_id desc
                """, new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    public Optional<InvestigationReport> findLatestByCaseId(long caseId) {
        return findByCaseId(caseId).stream().findFirst();
    }

    private MapSqlParameterSource params(InvestigationReport report) {
        return new MapSqlParameterSource()
                .addValue("caseId", report.caseId())
                .addValue("reportStatus", report.reportStatus().name())
                .addValue("reportTitle", report.reportTitle())
                .addValue("executiveSummary", report.executiveSummary())
                .addValue("keyRiskIndicators", report.keyRiskIndicators())
                .addValue("behaviorAnalysis", report.behaviorAnalysis())
                .addValue("relationshipAnalysis", report.relationshipAnalysis())
                .addValue("timelineObservations", report.timelineObservations())
                .addValue("possibleRiskPatterns", report.possibleRiskPatterns())
                .addValue("recommendations", report.recommendations())
                .addValue("modelName", report.modelName())
                .addValue("templateCode", report.templateCode())
                .addValue("templateVersion", report.templateVersion())
                .addValue("generatedAt", Timestamp.from(report.generatedAt()))
                .addValue("rawResponse", report.rawResponse());
    }

    private static final class InvestigationReportRowMapper implements RowMapper<InvestigationReport> {
        @Override
        public InvestigationReport mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InvestigationReport(
                    rs.getLong("report_id"),
                    rs.getLong("case_id"),
                    InvestigationReportStatus.valueOf(rs.getString("report_status")),
                    rs.getString("report_title"),
                    rs.getString("executive_summary"),
                    rs.getString("key_risk_indicators"),
                    rs.getString("behavior_analysis"),
                    rs.getString("relationship_analysis"),
                    rs.getString("timeline_observations"),
                    rs.getString("possible_risk_patterns"),
                    rs.getString("recommendations"),
                    rs.getString("model_name"),
                    rs.getString("template_code"),
                    rs.getInt("template_version"),
                    rs.getTimestamp("generated_at").toInstant(),
                    rs.getString("raw_response"));
        }
    }
}
