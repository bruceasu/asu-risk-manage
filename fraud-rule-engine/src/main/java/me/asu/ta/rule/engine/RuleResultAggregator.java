package me.asu.ta.rule.engine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class RuleResultAggregator {

    public RuleEngineResult aggregate(RuleEvaluationContext context, List<RuleEvaluationResult> evaluations) {
        List<RuleEvaluationResult> hits = new ArrayList<>();
        List<String> reasonCodes = new ArrayList<>();
        int totalScore = 0;
        for (RuleEvaluationResult evaluation : evaluations) {
            if (!evaluation.hit()) {
                continue;
            }
            hits.add(evaluation);
            totalScore += evaluation.score();
            if (evaluation.reasonCode() != null && !evaluation.reasonCode().isBlank()) {
                reasonCodes.add(evaluation.reasonCode());
            }
        }
        return new RuleEngineResult(
                context.accountId(),
                context.evaluationMode(),
                Instant.now(),
                totalScore,
                hits,
                reasonCodes);
    }
}
