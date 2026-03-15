package me.asu.ta.casemanagement.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class RuleSummaryBuilder {
    private final ObjectMapper objectMapper;

    public RuleSummaryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CaseRuleHit> build(long caseId, RuleEngineResult ruleEngineResult, Instant createdAt) {
        List<CaseRuleHit> hits = new ArrayList<>();
        for (RuleEvaluationResult hit : ruleEngineResult.hits()) {
            hits.add(new CaseRuleHit(
                    0L,
                    caseId,
                    hit.ruleCode(),
                    hit.ruleVersion(),
                    hit.severity(),
                    hit.score(),
                    hit.reasonCode() == null ? hit.ruleCode() : hit.reasonCode(),
                    hit.message(),
                    toJson(hit.evidence()),
                    createdAt));
        }
        return hits;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize case payload", ex);
        }
    }
}
