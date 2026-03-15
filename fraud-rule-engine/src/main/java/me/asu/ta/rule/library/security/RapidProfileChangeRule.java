package me.asu.ta.rule.library.security;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class RapidProfileChangeRule implements FraudRule {
    private static final String RULE_CODE = "RAPID_PROFILE_CHANGE";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.SECURITY;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        boolean requireRapidProfileChangeFlag = RuleSupport.booleanParam(config, "requireRapidProfileChangeFlag", true);
        boolean actual = RuleSupport.booleanValue(snapshot.rapidProfileChangeFlag24h());
        boolean hit = !requireRapidProfileChangeFlag || actual;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "RAPID_PROFILE_CHANGE",
                hit ? "Rapid profile change detected" : "Rapid profile change not detected",
                RuleSupport.evidence(
                        "rapidProfileChangeFlag24h", actual,
                        "requireRapidProfileChangeFlag", requireRapidProfileChangeFlag));
    }
}
