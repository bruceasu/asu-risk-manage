package me.asu.ta.ai.model;

import java.time.Instant;

public record AiGenerationJob(
        Long jobId,
        String jobType,
        Instant startedAt,
        Instant finishedAt,
        AiJobStatus status,
        Integer targetCaseCount,
        Integer processedCaseCount,
        Integer failedCaseCount,
        String errorMessage
) {
}
