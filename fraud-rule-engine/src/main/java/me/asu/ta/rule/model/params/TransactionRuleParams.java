package me.asu.ta.rule.model.params;

/**
 * Typed parameters for transaction rule family.
 */
public record TransactionRuleParams(
        Integer maxDelayMinutes,
        Integer minDepositCount24h,
        Integer minWithdrawCount24h,
        Integer minRewardTransactionCount30d,
        Double maxRewardWithdrawDelayHours30d
) implements RuleParameters {
    public TransactionRuleParams {
        validateNonNegative(maxDelayMinutes, "maxDelayMinutes");
        validateNonNegative(minDepositCount24h, "minDepositCount24h");
        validateNonNegative(minWithdrawCount24h, "minWithdrawCount24h");
        validateNonNegative(minRewardTransactionCount30d, "minRewardTransactionCount30d");
        if (maxRewardWithdrawDelayHours30d != null && maxRewardWithdrawDelayHours30d.doubleValue() < 0.0d) {
            throw new IllegalArgumentException("maxRewardWithdrawDelayHours30d must be greater than or equal to 0");
        }
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
