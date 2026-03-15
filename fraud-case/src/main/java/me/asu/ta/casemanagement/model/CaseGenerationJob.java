package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Batch generation job mapped to {@code case_generation_job}.
 */
public record CaseGenerationJob(
        long jobId,
        String jobType,
        Instant startedAt,
        Instant finishedAt,
        CaseJobStatus status,
        Integer targetAccountCount,
        Integer processedAccountCount,
        Integer failedAccountCount,
        String errorMessage
) {
    public CaseGenerationJob {
        Objects.requireNonNull(jobType, "jobType");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
    }
}
