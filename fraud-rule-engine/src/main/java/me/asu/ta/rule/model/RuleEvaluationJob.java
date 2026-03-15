package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Objects;

public record RuleEvaluationJob(
        long jobId,
        String jobType,
        Instant startedAt,
        Instant finishedAt,
        RuleJobStatus status,
        Integer targetAccountCount,
        Integer processedAccountCount,
        Integer hitAccountCount,
        Integer failedAccountCount,
        String errorMessage
) {
    public RuleEvaluationJob {
        Objects.requireNonNull(jobType, "jobType");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
    }
}
