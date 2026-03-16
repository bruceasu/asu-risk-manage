package me.asu.ta.rule.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {
    private final RuleRegistry ruleRegistry;
    private final RuleResultAggregator resultAggregator;

    public RuleEngine(RuleRegistry ruleRegistry, RuleResultAggregator resultAggregator) {
        this.ruleRegistry = ruleRegistry;
        this.resultAggregator = resultAggregator;
    }

    public RuleEngineResult evaluate(RuleEvaluationContext context, Map<String, RuleConfig> activeConfigs) {
        List<RuleEvaluationResult> evaluations = new ArrayList<>();
        for (RuleConfig config : activeConfigs.values()) {
            ruleRegistry.findRule(config.ruleCode())
                    .map(rule -> rule.evaluate(context, config))
                    .ifPresent(evaluations::add);
        }
        return resultAggregator.aggregate(context, evaluations);
    }
}
