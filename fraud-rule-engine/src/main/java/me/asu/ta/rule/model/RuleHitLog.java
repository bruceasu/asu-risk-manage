package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Objects;

public record RuleHitLog(
        long hitId,
        String accountId,
        String ruleCode,
        int ruleVersion,
        Instant hitTime,
        int score,
        String reasonCode,
        String evidenceJson,
        int featureVersion,
        EvaluationMode evaluationMode
) {
    public RuleHitLog {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(hitTime, "hitTime");
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(evaluationMode, "evaluationMode");
    }
}
