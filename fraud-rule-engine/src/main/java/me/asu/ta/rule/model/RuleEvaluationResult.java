package me.asu.ta.rule.model;

import java.util.Map;
import java.util.Objects;

public record RuleEvaluationResult(
        String ruleCode,
        int ruleVersion,
        boolean hit,
        RuleSeverity severity,
        int score,
        String reasonCode,
        String message,
        Map<String, Object> evidence
) {
    public RuleEvaluationResult {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(severity, "severity");
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
