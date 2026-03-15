package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Objects;

public record RuleVersion(
        String ruleCode,
        int version,
        String parameterJson,
        int scoreWeight,
        boolean enabled,
        Instant effectiveFrom,
        Instant effectiveTo,
        Instant createdAt,
        String createdBy,
        String changeNote
) {
    public RuleVersion {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(parameterJson, "parameterJson");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
