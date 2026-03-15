package me.asu.ta.rule.model.params;

/**
 * Typed parameters for device rule family.
 */
public record DeviceRuleParams(
        Integer minSharedDeviceAccounts7d,
        Integer minDeviceSwitchCount24h,
        Integer minUniqueDeviceCount7d
) implements RuleParameters {
    public DeviceRuleParams {
        validateNonNegative(minSharedDeviceAccounts7d, "minSharedDeviceAccounts7d");
        validateNonNegative(minDeviceSwitchCount24h, "minDeviceSwitchCount24h");
        validateNonNegative(minUniqueDeviceCount7d, "minUniqueDeviceCount7d");
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
