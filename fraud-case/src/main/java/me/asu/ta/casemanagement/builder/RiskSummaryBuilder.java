package me.asu.ta.casemanagement.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.risk.model.RiskScoreResult;
import org.springframework.stereotype.Component;

@Component
public class RiskSummaryBuilder {
    private final ObjectMapper objectMapper;

    public RiskSummaryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CaseRiskSummary build(long caseId, RiskScoreResult riskScoreResult) {
        return new CaseRiskSummary(
                caseId,
                toJson(riskScoreResult.scoreBreakdown()),
                riskScoreResult.scoreBreakdown().ruleScore(),
                riskScoreResult.scoreBreakdown().graphScore(),
                riskScoreResult.scoreBreakdown().anomalyScore(),
                riskScoreResult.scoreBreakdown().behaviorScore(),
                riskScoreResult.generatedAt());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize case payload", ex);
        }
    }
}
