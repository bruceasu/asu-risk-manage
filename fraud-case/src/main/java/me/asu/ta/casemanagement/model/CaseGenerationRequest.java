package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.rule.model.EvaluationMode;

/**
 * Input contract for realtime and batch case generation.
 */
public record CaseGenerationRequest(
        String accountId,
        AccountFeatureSnapshot snapshot,
        GraphRiskSignal graphRiskSignal,
        MlAnomalySignal mlAnomalySignal,
        Map<String, Object> contextSignals,
        EvaluationMode evaluationMode,
        Instant evaluationTime
) {
    public CaseGenerationRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(snapshot, "snapshot");
        contextSignals = contextSignals == null ? Map.of() : Map.copyOf(contextSignals);
    }

    public EvaluationMode resolvedEvaluationMode() {
        return evaluationMode == null ? EvaluationMode.REALTIME : evaluationMode;
    }

    public Instant resolvedEvaluationTime() {
        return evaluationTime == null ? Instant.now() : evaluationTime;
    }
}
