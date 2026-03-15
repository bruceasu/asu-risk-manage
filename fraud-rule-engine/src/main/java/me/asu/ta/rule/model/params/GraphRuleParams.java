package me.asu.ta.rule.model.params;

/**
 * Typed parameters for graph rule family.
 */
public record GraphRuleParams(
        Integer minRiskNeighborCount30d,
        Integer minGraphClusterSize30d
) implements RuleParameters {
    public GraphRuleParams {
        validateNonNegative(minRiskNeighborCount30d, "minRiskNeighborCount30d");
        validateNonNegative(minGraphClusterSize30d, "minGraphClusterSize30d");
    }

    private static void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
