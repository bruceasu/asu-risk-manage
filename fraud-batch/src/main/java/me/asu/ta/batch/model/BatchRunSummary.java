package me.asu.ta.batch.model;

import java.time.Instant;
import java.util.List;

public record BatchRunSummary(
        String jobName,
        Instant startedAt,
        Instant finishedAt,
        List<BatchRunStage> stages
) {
    public record BatchRunStage(
            String stageName,
            long jobId,
            String status,
            Integer processedCount,
            Integer failedCount
    ) {
    }
}
