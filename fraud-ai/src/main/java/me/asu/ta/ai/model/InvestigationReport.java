package me.asu.ta.ai.model;

import java.time.Instant;
import java.util.Objects;

public record InvestigationReport(
        Long reportId,
        long caseId,
        InvestigationReportStatus reportStatus,
        String reportTitle,
        String executiveSummary,
        String keyRiskIndicators,
        String behaviorAnalysis,
        String relationshipAnalysis,
        String timelineObservations,
        String possibleRiskPatterns,
        String recommendations,
        String modelName,
        String templateCode,
        int templateVersion,
        Instant generatedAt,
        String rawResponse
) {
    public InvestigationReport {
        Objects.requireNonNull(reportStatus, "reportStatus");
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(templateCode, "templateCode");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(rawResponse, "rawResponse");
    }
}
