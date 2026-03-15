package me.asu.ta.risk.model;

import java.time.Instant;
import java.util.Objects;
import me.asu.ta.rule.model.RuleSeverity;

/**
 * Human-readable reason metadata stored in risk_reason_mapping.
 */
public record RiskReasonMapping(
        String reasonCode,
        String reasonTitle,
        String reasonDescription,
        RuleSeverity severity,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public RiskReasonMapping {
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(reasonTitle, "reasonTitle");
        Objects.requireNonNull(reasonDescription, "reasonDescription");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
