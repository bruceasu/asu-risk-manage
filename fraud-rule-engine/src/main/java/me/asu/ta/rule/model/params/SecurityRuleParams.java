package me.asu.ta.rule.model.params;

/**
 * Typed parameters for security rule family.
 */
public record SecurityRuleParams(
        Boolean requireRapidProfileChangeFlag,
        Boolean requireSecurityChangeBeforeWithdrawFlag
) implements RuleParameters {
}
