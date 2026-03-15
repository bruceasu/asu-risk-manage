package me.asu.ta.risk.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Database-backed weighting profile used by the transparent final score formula.
 */
public record RiskWeightProfile(
        String profileName,
        double ruleWeight,
        double graphWeight,
        double anomalyWeight,
        double behaviorWeight,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public RiskWeightProfile {
        Objects.requireNonNull(profileName, "profileName");
        validateWeight(ruleWeight, "ruleWeight");
        validateWeight(graphWeight, "graphWeight");
        validateWeight(anomalyWeight, "anomalyWeight");
        validateWeight(behaviorWeight, "behaviorWeight");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static void validateWeight(double value, String field) {
        if (value < 0.0d || value > 1.0d) {
            throw new IllegalArgumentException(field + " must be between 0.0 and 1.0");
        }
    }
}
