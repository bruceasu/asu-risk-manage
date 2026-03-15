package me.asu.ta.rule.library;

import java.util.LinkedHashMap;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import me.asu.ta.rule.model.params.CompositeRuleParams;
import me.asu.ta.rule.model.params.DeviceRuleParams;
import me.asu.ta.rule.model.params.GraphRuleParams;
import me.asu.ta.rule.model.params.LoginRuleParams;
import me.asu.ta.rule.model.params.SecurityRuleParams;
import me.asu.ta.rule.model.params.TransactionRuleParams;

public final class RuleSupport {
    private RuleSupport() {
    }

    public static AccountFeatureSnapshot snapshot(RuleEvaluationContext context) {
        Object snapshot = context.attributes().get("snapshot");
        if (snapshot instanceof AccountFeatureSnapshot accountFeatureSnapshot) {
            return accountFeatureSnapshot;
        }
        throw new IllegalArgumentException("RuleEvaluationContext.attributes['snapshot'] must contain AccountFeatureSnapshot");
    }

    public static int intParam(RuleConfig config, String key, int defaultValue) {
        Object value = config.parameters().get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public static double doubleParam(RuleConfig config, String key, double defaultValue) {
        Object value = config.parameters().get(key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public static boolean booleanParam(RuleConfig config, String key, boolean defaultValue) {
        Object value = config.parameters().get(key);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    public static LoginRuleParams loginParams(RuleConfig config) {
        return config.typedParameters(LoginRuleParams.class).orElse(new LoginRuleParams(null, null, null));
    }

    public static TransactionRuleParams transactionParams(RuleConfig config) {
        return config.typedParameters(TransactionRuleParams.class).orElse(new TransactionRuleParams(null, null, null, null, null));
    }

    public static DeviceRuleParams deviceParams(RuleConfig config) {
        return config.typedParameters(DeviceRuleParams.class).orElse(new DeviceRuleParams(null, null, null));
    }

    public static SecurityRuleParams securityParams(RuleConfig config) {
        return config.typedParameters(SecurityRuleParams.class).orElse(new SecurityRuleParams(null, null));
    }

    public static GraphRuleParams graphParams(RuleConfig config) {
        return config.typedParameters(GraphRuleParams.class).orElse(new GraphRuleParams(null, null));
    }

    public static CompositeRuleParams compositeParams(RuleConfig config) {
        return config.typedParameters(CompositeRuleParams.class).orElse(new CompositeRuleParams(null, null, null));
    }

    public static RuleEvaluationResult result(
            String ruleCode,
            int ruleVersion,
            boolean hit,
            RuleSeverity severity,
            int score,
            String reasonCode,
            String message,
            Map<String, Object> evidence) {
        return new RuleEvaluationResult(ruleCode, ruleVersion, hit, severity, score, reasonCode, message, evidence);
    }

    public static Map<String, Object> evidence(Object... keyValues) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            evidence.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return evidence;
    }

    public static int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    public static double doubleValue(Double value) {
        return value == null ? 0.0d : value;
    }

    public static boolean booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
