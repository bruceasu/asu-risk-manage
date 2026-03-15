package me.asu.ta.rule.library.composite;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class AtoSuspicionRule implements FraudRule {
    private static final String RULE_CODE = "ATO_SUSPICION_COMPOSITE";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.COMPOSITE;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        int minNewDeviceLoginCount7d = RuleSupport.intParam(config, "minNewDeviceLoginCount7d", 1);
        int minHighRiskIpLoginCount24h = RuleSupport.intParam(config, "minHighRiskIpLoginCount24h", 1);
        boolean requireSecurityChangeBeforeWithdrawFlag =
                RuleSupport.booleanParam(config, "requireSecurityChangeBeforeWithdrawFlag", true);

        int newDeviceLoginCount = RuleSupport.intValue(snapshot.newDeviceLoginCount7d());
        int highRiskIpLoginCount = RuleSupport.intValue(snapshot.highRiskIpLoginCount24h());
        boolean securityChangeBeforeWithdraw = RuleSupport.booleanValue(snapshot.securityChangeBeforeWithdrawFlag24h());
        boolean hit = newDeviceLoginCount >= minNewDeviceLoginCount7d
                && highRiskIpLoginCount >= minHighRiskIpLoginCount24h
                && (!requireSecurityChangeBeforeWithdrawFlag || securityChangeBeforeWithdraw);

        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "ATO_SUSPICION_COMPOSITE",
                hit ? "Composite account takeover suspicion detected" : "Composite account takeover suspicion not detected",
                RuleSupport.evidence(
                        "newDeviceLoginCount7d", newDeviceLoginCount,
                        "highRiskIpLoginCount24h", highRiskIpLoginCount,
                        "securityChangeBeforeWithdrawFlag24h", securityChangeBeforeWithdraw,
                        "minNewDeviceLoginCount7d", minNewDeviceLoginCount7d,
                        "minHighRiskIpLoginCount24h", minHighRiskIpLoginCount24h,
                        "requireSecurityChangeBeforeWithdrawFlag", requireSecurityChangeBeforeWithdrawFlag));
    }
}
