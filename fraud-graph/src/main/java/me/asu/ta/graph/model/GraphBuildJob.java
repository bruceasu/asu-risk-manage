package me.asu.ta.graph.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Row model mapped to {@code graph_build_job}.
 *
 * @param jobId surrogate primary key
 * @param jobType build mode such as full window batch build
 * @param graphWindowStart inclusive graph build window start
 * @param graphWindowEnd exclusive graph build window end
 * @param startedAt job start time
 * @param finishedAt job finish time when available
 * @param status current job execution status
 * @param processedAccountCount number of accounts covered by the generated clusters
 * @param generatedEdgeCount number of graph edges persisted for the window
 * @param generatedClusterCount number of cluster summaries generated for the window
 * @param errorMessage condensed failure detail when the job is not fully successful
 */
public record GraphBuildJob(
        long jobId,
        String jobType,
        Instant graphWindowStart,
        Instant graphWindowEnd,
        Instant startedAt,
        Instant finishedAt,
        GraphBuildJobStatus status,
        Integer processedAccountCount,
        Integer generatedEdgeCount,
        Integer generatedClusterCount,
        String errorMessage
) {
    public GraphBuildJob {
        Objects.requireNonNull(jobType, "jobType");
        Objects.requireNonNull(graphWindowStart, "graphWindowStart");
        Objects.requireNonNull(graphWindowEnd, "graphWindowEnd");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
    }
}
