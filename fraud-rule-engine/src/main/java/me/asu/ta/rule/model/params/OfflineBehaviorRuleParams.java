package me.asu.ta.rule.model.params;

/**
 * Typed parameters for offline behavior context rules.
 */
public record OfflineBehaviorRuleParams(
        Double minCoordinatedTradingScore,
        Integer minSimilarAccountCount,
        Integer minBehaviorClusterSize
) implements RuleParameters {
    public OfflineBehaviorRuleParams {
        validateNonNegative(minCoordinatedTradingScore, "minCoordinatedTradingScore");
        validateNonNegative(minSimilarAccountCount, "minSimilarAccountCount");
        validateNonNegative(minBehaviorClusterSize, "minBehaviorClusterSize");
    }

    private static void validateNonNegative(Double value, String fieldName) {
        if (value != null && value.doubleValue() < 0.0d) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
