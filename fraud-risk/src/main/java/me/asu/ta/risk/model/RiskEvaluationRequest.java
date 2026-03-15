package me.asu.ta.risk.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.model.EvaluationMode;

/**
 * Input contract for single-account or batch risk evaluation.
 */
public record RiskEvaluationRequest(
        String accountId,
        AccountFeatureSnapshot snapshot,
        GraphRiskSignal graphRiskSignal,
        MlAnomalySignal mlAnomalySignal,
        Map<String, Object> contextSignals,
        EvaluationMode evaluationMode,
        Instant evaluationTime
) {
    public RiskEvaluationRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(snapshot, "snapshot");
        contextSignals = contextSignals == null ? Map.of() : Map.copyOf(contextSignals);
    }

    public GraphRiskSignal resolvedGraphRiskSignal() {
        return graphRiskSignal == null ? GraphRiskSignal.empty() : graphRiskSignal;
    }

    public EvaluationMode resolvedEvaluationMode() {
        return evaluationMode == null ? EvaluationMode.REALTIME : evaluationMode;
    }

    public Instant resolvedEvaluationTime() {
        return evaluationTime == null ? Instant.now() : evaluationTime;
    }
}
