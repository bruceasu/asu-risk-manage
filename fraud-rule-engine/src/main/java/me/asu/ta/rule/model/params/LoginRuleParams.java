package me.asu.ta.rule.model.params;

/**
 * Typed parameters for login rule family.
 */
public record LoginRuleParams(
        Integer minHighRiskIpLoginCount24h,
        Integer minLoginFailureCount24h,
        Double minLoginFailureRate24h
) implements RuleParameters {
    public LoginRuleParams {
        validateNonNegative(minHighRiskIpLoginCount24h, "minHighRiskIpLoginCount24h");
        validateNonNegative(minLoginFailureCount24h, "minLoginFailureCount24h");
        if (minLoginFailureRate24h != null
                && (minLoginFailureRate24h.doubleValue() < 0.0d || minLoginFailureRate24h.doubleValue() > 1.0d)) {
            throw new IllegalArgumentException("minLoginFailureRate24h must be between 0.0 and 1.0");
        }
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
