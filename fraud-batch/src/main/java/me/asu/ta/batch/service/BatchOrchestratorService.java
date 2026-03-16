package me.asu.ta.batch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.batch.model.BatchRunSummary;
import me.asu.ta.casemanagement.job.CaseGenerationJobRunner;
import me.asu.ta.casemanagement.model.CaseGenerationJob;
import me.asu.ta.feature.job.FeatureGenerationJobRunner;
import me.asu.ta.feature.model.FeatureGenerationJob;
import me.asu.ta.risk.job.RiskEvaluationJobRunner;
import me.asu.ta.risk.model.RiskEvaluationJob;
import org.springframework.stereotype.Service;

@Service
public class BatchOrchestratorService {
    private final FeatureGenerationJobRunner featureGenerationJobRunner;
    private final RiskEvaluationJobRunner riskEvaluationJobRunner;
    private final CaseGenerationJobRunner caseGenerationJobRunner;

    public BatchOrchestratorService(
            FeatureGenerationJobRunner featureGenerationJobRunner,
            RiskEvaluationJobRunner riskEvaluationJobRunner,
            CaseGenerationJobRunner caseGenerationJobRunner) {
        this.featureGenerationJobRunner = featureGenerationJobRunner;
        this.riskEvaluationJobRunner = riskEvaluationJobRunner;
        this.caseGenerationJobRunner = caseGenerationJobRunner;
    }

    public BatchRunSummary run(BatchJobType jobType) {
        return switch (jobType) {
            case FEATURE -> singleJobSummary("FEATURE_REFRESH", toStage(featureGenerationJobRunner.runFullGeneration()));
            case RISK -> singleJobSummary("RISK_EVALUATION", toStage(riskEvaluationJobRunner.runFullEvaluation()));
            case CASE -> singleJobSummary("CASE_GENERATION", toStage(caseGenerationJobRunner.runFullGeneration()));
            case PIPELINE -> runPipeline();
        };
    }

    private BatchRunSummary runPipeline() {
        Instant startedAt = Instant.now();
        List<BatchRunSummary.BatchRunStage> stages = new ArrayList<>();
        stages.add(toStage(featureGenerationJobRunner.runFullGeneration()));
        stages.add(toStage(riskEvaluationJobRunner.runFullEvaluation()));
        stages.add(toStage(caseGenerationJobRunner.runFullGeneration()));
        return new BatchRunSummary("FULL_RISK_PIPELINE", startedAt, Instant.now(), List.copyOf(stages));
    }

    private BatchRunSummary singleJobSummary(String jobName, BatchRunSummary.BatchRunStage stage) {
        Instant now = Instant.now();
        return new BatchRunSummary(jobName, now, now, List.of(stage));
    }

    private BatchRunSummary.BatchRunStage toStage(FeatureGenerationJob job) {
        return new BatchRunSummary.BatchRunStage(
                "FEATURE_REFRESH",
                job.jobId(),
                job.status(),
                job.processedAccountCount(),
                job.failedAccountCount());
    }

    private BatchRunSummary.BatchRunStage toStage(RiskEvaluationJob job) {
        return new BatchRunSummary.BatchRunStage(
                "RISK_EVALUATION",
                job.jobId(),
                job.status().name(),
                job.processedAccountCount(),
                job.failedAccountCount());
    }

    private BatchRunSummary.BatchRunStage toStage(CaseGenerationJob job) {
        return new BatchRunSummary.BatchRunStage(
                "CASE_GENERATION",
                job.jobId(),
                job.status().name(),
                job.processedAccountCount(),
                job.failedAccountCount());
    }
}
