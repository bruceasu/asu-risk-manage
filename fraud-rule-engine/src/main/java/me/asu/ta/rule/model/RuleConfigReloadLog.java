package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Objects;

public record RuleConfigReloadLog(
        long reloadId,
        Instant reloadTime,
        String status,
        Integer loadedRuleCount,
        String errorMessage
) {
    public RuleConfigReloadLog {
        Objects.requireNonNull(reloadTime, "reloadTime");
        Objects.requireNonNull(status, "status");
    }
}
