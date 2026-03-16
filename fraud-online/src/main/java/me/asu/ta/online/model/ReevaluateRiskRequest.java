package me.asu.ta.online.model;

import me.asu.ta.rule.model.EvaluationMode;

public record ReevaluateRiskRequest(
        EvaluationMode evaluationMode,
        String source
) {
}
