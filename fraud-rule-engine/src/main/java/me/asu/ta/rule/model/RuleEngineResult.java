package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RuleEngineResult(
        String accountId,
        EvaluationMode evaluationMode,
        Instant evaluationTime,
        int totalScore,
        List<RuleEvaluationResult> hits,
        List<String> reasonCodes
) {
    public RuleEngineResult {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(evaluationMode, "evaluationMode");
        Objects.requireNonNull(evaluationTime, "evaluationTime");
        hits = hits == null ? List.of() : List.copyOf(hits);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
