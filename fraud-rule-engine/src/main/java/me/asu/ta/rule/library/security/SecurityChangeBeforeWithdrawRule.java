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
public class SecurityChangeBeforeWithdrawRule implements FraudRule {
    private static final String RULE_CODE = "SECURITY_CHANGE_BEFORE_WITHDRAW";

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
        boolean requireFlag = RuleSupport.booleanParam(config, "requireSecurityChangeBeforeWithdrawFlag", true);
        boolean actual = RuleSupport.booleanValue(snapshot.securityChangeBeforeWithdrawFlag24h());
        boolean hit = !requireFlag || actual;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "SECURITY_CHANGE_BEFORE_WITHDRAW",
                hit ? "Security change before withdrawal detected" : "Security change before withdrawal not detected",
                RuleSupport.evidence(
                        "securityChangeBeforeWithdrawFlag24h", actual,
                        "requireSecurityChangeBeforeWithdrawFlag", requireFlag));
    }
}
