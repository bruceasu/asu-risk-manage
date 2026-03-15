package me.asu.ta.risk.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Behavior score plus explicit evidence and reason codes derived from feature signals.
 */
public record BehaviorRiskSignal(
        double behaviorScore,
        Map<String, Object> evidence,
        List<String> reasonCodes
) {
    public BehaviorRiskSignal {
        behaviorScore = Math.max(0.0d, Math.min(100.0d, behaviorScore));
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
