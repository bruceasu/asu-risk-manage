package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;
import me.asu.ta.rule.model.RuleSeverity;

/**
 * Rule hit record mapped to {@code case_rule_hit}.
 */
public record CaseRuleHit(
        long caseRuleHitId,
        long caseId,
        String ruleCode,
        int ruleVersion,
        RuleSeverity severity,
        int score,
        String reasonCode,
        String message,
        String evidenceJson,
        Instant createdAt
) {
    public CaseRuleHit {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
