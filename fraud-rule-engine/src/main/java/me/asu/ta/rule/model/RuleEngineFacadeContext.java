package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.feature.model.AccountFeatureSnapshot;

/**
 * Facade-friendly evaluation input used by external fraud modules.
 */
public record RuleEngineFacadeContext(
        AccountFeatureSnapshot snapshot,
        Instant evaluationTime,
        EvaluationMode evaluationMode,
        Map<String, Object> graphSignals,
        Map<String, Object> attributes
) {
    public RuleEngineFacadeContext {
        Objects.requireNonNull(snapshot, "snapshot");
        graphSignals = graphSignals == null ? Map.of() : Map.copyOf(graphSignals);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public Instant resolvedEvaluationTime() {
        return evaluationTime == null ? Instant.now() : evaluationTime;
    }

    public EvaluationMode resolvedEvaluationMode(EvaluationMode defaultMode) {
        return evaluationMode == null ? defaultMode : evaluationMode;
    }
}
