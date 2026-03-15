package me.asu.ta.graph.job;

import java.time.Instant;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.model.GraphBuildJob;
import me.asu.ta.graph.model.GraphBuildJobStatus;
import me.asu.ta.graph.repository.GraphBuildJobRepository;
import me.asu.ta.graph.service.GraphBuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GraphBuildJobRunner {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuildJobRunner.class);

    private final GraphBuildService graphBuildService;
    private final GraphBuildJobRepository graphBuildJobRepository;

    public GraphBuildJobRunner(GraphBuildService graphBuildService, GraphBuildJobRepository graphBuildJobRepository) {
        this.graphBuildService = graphBuildService;
        this.graphBuildJobRepository = graphBuildJobRepository;
    }

    public GraphBuildJob runWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        Instant startedAt = Instant.now();
        GraphBuildJob runningJob = graphBuildJobRepository.createJob(new GraphBuildJob(
                0L,
                "BATCH_GRAPH_BUILD",
                graphWindowStart,
                graphWindowEnd,
                startedAt,
                null,
                GraphBuildJobStatus.RUNNING,
                0,
                0,
                0,
                null));
        logger.info("Started graph build job {} for window {} - {}", runningJob.jobId(), graphWindowStart, graphWindowEnd);
        try {
            GraphAnalysisSnapshot snapshot = graphBuildService.buildWindow(graphWindowStart, graphWindowEnd);
            GraphBuildJob completedJob = new GraphBuildJob(
                    runningJob.jobId(),
                    runningJob.jobType(),
                    runningJob.graphWindowStart(),
                    runningJob.graphWindowEnd(),
                    runningJob.startedAt(),
                    Instant.now(),
                    GraphBuildJobStatus.SUCCESS,
                    distinctAccountCount(snapshot),
                    snapshot.edges().size(),
                    snapshot.graphRiskSummaries().size(),
                    null);
            graphBuildJobRepository.updateJobStatus(completedJob);
            logger.info(
                    "Finished graph build job {} with processedAccounts={}, edges={}, clusters={}",
                    completedJob.jobId(),
                    completedJob.processedAccountCount(),
                    completedJob.generatedEdgeCount(),
                    completedJob.generatedClusterCount());
            return completedJob;
        } catch (RuntimeException ex) {
            GraphBuildJob failedJob = new GraphBuildJob(
                    runningJob.jobId(),
                    runningJob.jobType(),
                    runningJob.graphWindowStart(),
                    runningJob.graphWindowEnd(),
                    runningJob.startedAt(),
                    Instant.now(),
                    GraphBuildJobStatus.FAILED,
                    runningJob.processedAccountCount(),
                    runningJob.generatedEdgeCount(),
                    runningJob.generatedClusterCount(),
                    ex.getMessage());
            graphBuildJobRepository.updateJobStatus(failedJob);
            logger.error("Graph build job {} failed", runningJob.jobId(), ex);
            throw ex;
        }
    }

    private int distinctAccountCount(GraphAnalysisSnapshot snapshot) {
        return (int) snapshot.clusters().stream().map(me.asu.ta.graph.model.GraphClusterMembership::accountId).distinct().count();
    }
}
