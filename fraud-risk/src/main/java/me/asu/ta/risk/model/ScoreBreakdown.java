package me.asu.ta.risk.model;

import java.util.Objects;

/**
 * Explicit risk score components used for audit and explanation.
 */
public record ScoreBreakdown(
        double ruleScore,
        double graphScore,
        double anomalyScore,
        double behaviorScore,
        double finalScore,
        String profileName
) {
    public ScoreBreakdown {
        ruleScore = clamp(ruleScore);
        graphScore = clamp(graphScore);
        anomalyScore = clamp(anomalyScore);
        behaviorScore = clamp(behaviorScore);
        finalScore = clamp(finalScore);
        Objects.requireNonNull(profileName, "profileName");
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(100.0d, value));
    }
}
