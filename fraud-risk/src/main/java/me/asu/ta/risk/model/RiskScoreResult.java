package me.asu.ta.risk.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import me.asu.ta.rule.model.EvaluationMode;

/**
 * Persisted risk result mapped to risk_score_result.
 */
public record RiskScoreResult(
        long scoreId,
        String accountId,
        double riskScore,
        RiskLevel riskLevel,
        String profileName,
        int featureVersion,
        Instant generatedAt,
        EvaluationMode evaluationMode,
        List<String> topReasonCodes,
        ScoreBreakdown scoreBreakdown
) {
    public RiskScoreResult {
        Objects.requireNonNull(accountId, "accountId");
        riskScore = Math.max(0.0d, Math.min(100.0d, riskScore));
        Objects.requireNonNull(riskLevel, "riskLevel");
        Objects.requireNonNull(profileName, "profileName");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(evaluationMode, "evaluationMode");
        topReasonCodes = topReasonCodes == null ? List.of() : List.copyOf(topReasonCodes);
        Objects.requireNonNull(scoreBreakdown, "scoreBreakdown");
    }
}
