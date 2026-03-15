package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Recommended action mapped to {@code case_recommended_action}.
 */
public record CaseRecommendedAction(
        long caseActionId,
        long caseId,
        String actionCode,
        String actionReason,
        Instant createdAt
) {
    public CaseRecommendedAction {
        Objects.requireNonNull(actionCode, "actionCode");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
