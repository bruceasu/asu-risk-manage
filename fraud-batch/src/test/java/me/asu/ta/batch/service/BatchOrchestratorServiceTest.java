package me.asu.ta.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import me.asu.ta.batch.model.BatchRunSummary;
import me.asu.ta.casemanagement.job.CaseGenerationJobRunner;
import me.asu.ta.casemanagement.model.CaseGenerationJob;
import me.asu.ta.casemanagement.model.CaseJobStatus;
import me.asu.ta.feature.job.FeatureGenerationJobRunner;
import me.asu.ta.feature.model.FeatureGenerationJob;
import me.asu.ta.risk.job.RiskEvaluationJobRunner;
import me.asu.ta.risk.model.RiskEvaluationJob;
import me.asu.ta.risk.model.RiskJobStatus;
import org.junit.jupiter.api.Test;

class BatchOrchestratorServiceTest {
    @Test
    void shouldRunPipelineInOrder() {
        FeatureGenerationJobRunner featureRunner = new FeatureGenerationJobRunner(null, null, null, null) {
            @Override
            public FeatureGenerationJob runFullGeneration() {
                return new FeatureGenerationJob(11L, "FEATURE", 1, Instant.now(), Instant.now(), "COMPLETED", 10, 10, 0, null);
            }
        };
        RiskEvaluationJobRunner riskRunner = new RiskEvaluationJobRunner(null, null, null, null, null) {
            @Override
            public RiskEvaluationJob runFullEvaluation() {
                return new RiskEvaluationJob(12L, "RISK", Instant.now(), Instant.now(), RiskJobStatus.COMPLETED, 10, 10, 0, null);
            }
        };
        CaseGenerationJobRunner caseRunner = new CaseGenerationJobRunner(null, null, null) {
            @Override
            public CaseGenerationJob runFullGeneration() {
                return new CaseGenerationJob(13L, "CASE", Instant.now(), Instant.now(), CaseJobStatus.COMPLETED, 8, 8, 0, null);
            }
        };

        BatchOrchestratorService service = new BatchOrchestratorService(featureRunner, riskRunner, caseRunner);
        BatchRunSummary summary = service.run(BatchJobType.PIPELINE);

        assertEquals("FULL_RISK_PIPELINE", summary.jobName());
        assertEquals(3, summary.stages().size());
        assertEquals("FEATURE_REFRESH", summary.stages().getFirst().stageName());
        assertEquals("RISK_EVALUATION", summary.stages().get(1).stageName());
        assertEquals("CASE_GENERATION", summary.stages().get(2).stageName());
    }
}
