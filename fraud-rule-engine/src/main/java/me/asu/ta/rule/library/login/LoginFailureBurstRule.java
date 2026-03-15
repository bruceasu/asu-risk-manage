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
public class LoginFailureBurstRule implements FraudRule {
    private static final String RULE_CODE = "LOGIN_FAILURE_BURST";

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
        int minLoginFailureCount24h = RuleSupport.intParam(config, "minLoginFailureCount24h", 20);
        double minLoginFailureRate24h = RuleSupport.doubleParam(config, "minLoginFailureRate24h", 0.8d);
        int failureCount = RuleSupport.intValue(snapshot.loginFailureCount24h());
        double failureRate = RuleSupport.doubleValue(snapshot.loginFailureRate24h());
        boolean hit = failureCount >= minLoginFailureCount24h && failureRate >= minLoginFailureRate24h;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "LOGIN_FAILURE_BURST",
                hit ? "Login failure burst detected" : "Login failure burst not detected",
                RuleSupport.evidence(
                        "loginFailureCount24h", failureCount,
                        "loginFailureRate24h", failureRate,
                        "minLoginFailureCount24h", minLoginFailureCount24h,
                        "minLoginFailureRate24h", minLoginFailureRate24h));
    }
}
