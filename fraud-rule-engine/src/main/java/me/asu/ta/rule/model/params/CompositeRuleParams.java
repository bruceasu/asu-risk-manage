package me.asu.ta.rule.model.params;

/**
 * Typed parameters for composite rule family.
 */
public record CompositeRuleParams(
        Integer minNewDeviceLoginCount7d,
        Integer minHighRiskIpLoginCount24h,
        Boolean requireSecurityChangeBeforeWithdrawFlag
) implements RuleParameters {
    public CompositeRuleParams {
        validateNonNegative(minNewDeviceLoginCount7d, "minNewDeviceLoginCount7d");
        validateNonNegative(minHighRiskIpLoginCount24h, "minHighRiskIpLoginCount24h");
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
