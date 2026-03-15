package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.rule.model.EvaluationMode;

/**
 * Main case record mapped to {@code investigation_case}.
 */
public record InvestigationCase(
        long caseId,
        String accountId,
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
    public InvestigationCase {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(caseStatus, "caseStatus");
        riskScore = Math.max(0.0d, Math.min(100.0d, riskScore));
        Objects.requireNonNull(riskLevel, "riskLevel");
        Objects.requireNonNull(profileName, "profileName");
        topReasonCodes = topReasonCodes == null ? List.of() : List.copyOf(topReasonCodes);
        Objects.requireNonNull(evaluationMode, "evaluationMode");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
