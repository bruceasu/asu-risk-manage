package me.asu.ta.rule.library.device;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class SharedDeviceClusterRule implements FraudRule {
    private static final String RULE_CODE = "SHARED_DEVICE_CLUSTER";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.DEVICE;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        int minSharedDeviceAccounts7d = RuleSupport.intParam(config, "minSharedDeviceAccounts7d", 5);
        int actual = RuleSupport.intValue(snapshot.sharedDeviceAccounts7d());
        boolean hit = actual >= minSharedDeviceAccounts7d;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "SHARED_DEVICE_CLUSTER",
                hit ? "Shared device cluster threshold reached" : "Shared device cluster threshold not reached",
                RuleSupport.evidence(
                        "sharedDeviceAccounts7d", actual,
                        "minSharedDeviceAccounts7d", minSharedDeviceAccounts7d));
    }
}
