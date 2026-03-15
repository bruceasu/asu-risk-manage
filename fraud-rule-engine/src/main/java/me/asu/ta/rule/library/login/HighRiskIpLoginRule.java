package me.asu.ta.rule.library.login;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class HighRiskIpLoginRule implements FraudRule {
    private static final String RULE_CODE = "HIGH_RISK_IP_LOGIN";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.LOGIN;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        int minHighRiskIpLoginCount24h = RuleSupport.intParam(config, "minHighRiskIpLoginCount24h", 1);
        int actual = RuleSupport.intValue(snapshot.highRiskIpLoginCount24h());
        boolean hit = actual >= minHighRiskIpLoginCount24h;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "HIGH_RISK_IP_LOGIN",
                hit ? "High risk IP login threshold reached" : "High risk IP login threshold not reached",
                RuleSupport.evidence(
                        "highRiskIpLoginCount24h", actual,
                        "minHighRiskIpLoginCount24h", minHighRiskIpLoginCount24h));
    }
}
