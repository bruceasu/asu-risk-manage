package me.asu.ta.risk.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Optional ML anomaly output normalized to the risk engine scale.
 */
public record MlAnomalySignal(
        double anomalyScoreRaw,
        double anomalyScoreNormalized,
        String modelName,
        Instant scoredAt
) {
    public MlAnomalySignal {
        anomalyScoreNormalized = Math.max(0.0d, Math.min(100.0d, anomalyScoreNormalized));
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(scoredAt, "scoredAt");
    }
}
