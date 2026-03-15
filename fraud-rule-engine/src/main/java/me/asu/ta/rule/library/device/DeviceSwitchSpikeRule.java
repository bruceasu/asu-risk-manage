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
public class DeviceSwitchSpikeRule implements FraudRule {
    private static final String RULE_CODE = "DEVICE_SWITCH_SPIKE";

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
        int minDeviceSwitchCount24h = RuleSupport.intParam(config, "minDeviceSwitchCount24h", 3);
        int minUniqueDeviceCount7d = RuleSupport.intParam(config, "minUniqueDeviceCount7d", 2);
        int deviceSwitchCount = RuleSupport.intValue(snapshot.deviceSwitchCount24h());
        int uniqueDeviceCount = RuleSupport.intValue(snapshot.uniqueDeviceCount7d());
        boolean hit = deviceSwitchCount >= minDeviceSwitchCount24h && uniqueDeviceCount >= minUniqueDeviceCount7d;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "DEVICE_SWITCH_SPIKE",
                hit ? "Device switch spike detected" : "Device switch spike not detected",
                RuleSupport.evidence(
                        "deviceSwitchCount24h", deviceSwitchCount,
                        "uniqueDeviceCount7d", uniqueDeviceCount,
                        "minDeviceSwitchCount24h", minDeviceSwitchCount24h,
                        "minUniqueDeviceCount7d", minUniqueDeviceCount7d));
    }
}
