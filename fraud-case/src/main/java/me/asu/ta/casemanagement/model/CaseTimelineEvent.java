package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Timeline event mapped to {@code case_timeline_event}.
 */
public record CaseTimelineEvent(
        long timelineEventId,
        long caseId,
        Instant eventTime,
        String eventType,
        String title,
        String description,
        String evidenceJson,
        Instant createdAt
) {
    public CaseTimelineEvent {
        Objects.requireNonNull(eventTime, "eventTime");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
