package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Risk summary mapped to {@code case_risk_summary}.
 */
public record CaseRiskSummary(
        long caseId,
        String scoreBreakdownJson,
        Double ruleScore,
        Double graphScore,
        Double anomalyScore,
        Double behaviorScore,
        Instant createdAt
) {
    public CaseRiskSummary {
        Objects.requireNonNull(scoreBreakdownJson, "scoreBreakdownJson");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
