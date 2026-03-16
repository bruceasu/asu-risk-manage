package me.asu.ta.online.model;

import java.time.Instant;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.rule.model.EvaluationMode;

public record CaseSummaryView(
        long caseId,
        CaseStatus caseStatus,
        double riskScore,
        RiskLevel riskLevel,
        String profileName,
        List<String> topReasonCodes,
        int featureVersion,
        EvaluationMode evaluationMode,
        Instant createdAt,
        Instant updatedAt
) {
}
