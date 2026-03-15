package me.asu.ta.rule.api;

import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;

public interface FraudRule {
    String ruleCode();

    RuleCategory category();

    RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config);
}
