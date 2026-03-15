package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Batch generation job mapped to the {@code feature_generation_job} table.
 *
 * @param jobId feature_generation_job.job_id
 * @param jobType feature_generation_job.job_type
 * @param featureVersion feature_generation_job.feature_version
 * @param startedAt feature_generation_job.started_at
 * @param finishedAt feature_generation_job.finished_at
 * @param status feature_generation_job.status
 * @param targetAccountCount feature_generation_job.target_account_count
 * @param processedAccountCount feature_generation_job.processed_account_count
 * @param failedAccountCount feature_generation_job.failed_account_count
 * @param errorMessage feature_generation_job.error_message
 */
public record FeatureGenerationJob(
        long jobId,
        String jobType,
        int featureVersion,
        Instant startedAt,
        Instant finishedAt,
        String status,
        Integer targetAccountCount,
        Integer processedAccountCount,
        Integer failedAccountCount,
        String errorMessage
) {
    public FeatureGenerationJob {
        Objects.requireNonNull(jobType, "jobType");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
    }
}
