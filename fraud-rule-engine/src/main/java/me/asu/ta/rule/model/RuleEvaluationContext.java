package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record RuleEvaluationContext(
        String accountId,
        Instant evaluationTime,
        int featureVersion,
        EvaluationMode evaluationMode,
        Map<String, Object> features,
        Map<String, Object> attributes
) {
    public RuleEvaluationContext {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(evaluationTime, "evaluationTime");
        Objects.requireNonNull(evaluationMode, "evaluationMode");
        features = features == null ? Map.of() : Map.copyOf(features);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
