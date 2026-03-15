package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Objects;

public record RuleDefinition(
        String ruleCode,
        String ruleName,
        RuleCategory category,
        String description,
        RuleSeverity severity,
        String ownerModule,
        int currentVersion,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public RuleDefinition {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(ruleName, "ruleName");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(ownerModule, "ownerModule");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
