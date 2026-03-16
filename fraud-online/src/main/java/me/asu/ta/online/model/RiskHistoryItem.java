package me.asu.ta.online.model;

import java.time.Instant;
import java.util.List;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.rule.model.EvaluationMode;

public record RiskHistoryItem(
        long scoreId,
        double riskScore,
        RiskLevel riskLevel,
        String profileName,
        int featureVersion,
        Instant generatedAt,
        EvaluationMode evaluationMode,
        List<String> topReasonCodes,
        Object scoreBreakdown
) {
}
