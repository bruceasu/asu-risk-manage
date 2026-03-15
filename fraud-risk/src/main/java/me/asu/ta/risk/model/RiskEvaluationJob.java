package me.asu.ta.risk.model;

import java.time.Instant;

/**
 * Batch risk evaluation job mapped to risk_evaluation_job.
 */
public record RiskEvaluationJob(
        long jobId,
        String jobType,
        Instant startedAt,
        Instant finishedAt,
        RiskJobStatus status,
        Integer targetAccountCount,
        Integer processedAccountCount,
        Integer failedAccountCount,
        String errorMessage
) {
}
