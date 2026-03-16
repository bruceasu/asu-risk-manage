package me.asu.ta.online.model;

import java.time.Instant;
import me.asu.ta.rule.model.EvaluationMode;

public record RuleHitSummary(
        long hitId,
        String ruleCode,
        int ruleVersion,
        Instant hitTime,
        int score,
        String reasonCode,
        String evidenceJson,
        int featureVersion,
        EvaluationMode evaluationMode
) {
}
