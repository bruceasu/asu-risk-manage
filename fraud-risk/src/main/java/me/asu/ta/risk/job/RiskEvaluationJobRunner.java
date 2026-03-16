package me.asu.ta.risk.job;

import java.time.Instant;
import java.util.List;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import me.asu.ta.risk.model.RiskEvaluationJob;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskJobStatus;
import me.asu.ta.risk.repository.RiskAccountBatchReader;
import me.asu.ta.risk.repository.RiskEvaluationJobRepository;
import me.asu.ta.risk.service.GraphRiskSignalResolver;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RiskEvaluationJobRunner {
    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationJobRunner.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final RiskAccountBatchReader batchReader;
    private final AccountFeatureSnapshotRepository snapshotRepository;
    private final RiskEvaluationJobRepository jobRepository;
    private final RiskEngineFacade riskEngineFacade;
    private final GraphRiskSignalResolver graphRiskSignalResolver;

    public RiskEvaluationJobRunner(
            RiskAccountBatchReader batchReader,
            AccountFeatureSnapshotRepository snapshotRepository,
            RiskEvaluationJobRepository jobRepository,
            RiskEngineFacade riskEngineFacade,
            GraphRiskSignalResolver graphRiskSignalResolver) {
        this.batchReader = batchReader;
        this.snapshotRepository = snapshotRepository;
        this.jobRepository = jobRepository;
        this.riskEngineFacade = riskEngineFacade;
        this.graphRiskSignalResolver = graphRiskSignalResolver;
    }

    public RiskEvaluationJob runFullEvaluation() {
        Instant startedAt = Instant.now();
        RiskEvaluationJob job = jobRepository.createJob(new RiskEvaluationJob(
                0L,
                "FULL_ACCOUNT_RISK_EVALUATION",
                startedAt,
                null,
                RiskJobStatus.RUNNING,
                batchReader.countAccounts(),
                0,
                0,
                null));
        int processed = 0;
        int failed = 0;
        String lastAccountId = null;
        try {
            while (true) {
                List<String> batchIds = batchReader.findNextAccountIds(lastAccountId, DEFAULT_BATCH_SIZE);
                if (batchIds.isEmpty()) {
                    break;
                }
                List<AccountFeatureSnapshot> snapshots = snapshotRepository.findBatch(batchIds);
                List<RiskEvaluationRequest> requests = snapshots.stream().map(this::toRequest).toList();
                riskEngineFacade.evaluateBatch(requests);
                processed += requests.size();
                lastAccountId = batchIds.get(batchIds.size() - 1);
                job = updateJob(job, RiskJobStatus.RUNNING, processed, failed, null, null);
                log.info("Risk evaluation job {} processed {}/{}", job.jobId(), processed, job.targetAccountCount());
            }
            return updateJob(job, RiskJobStatus.COMPLETED, processed, failed, null, Instant.now());
        } catch (RuntimeException ex) {
            failed += 1;
            log.warn("Risk evaluation job {} failed after {} accounts: {}", job.jobId(), processed, ex.getMessage());
            return updateJob(job, RiskJobStatus.FAILED, processed, failed, ex.getMessage(), Instant.now());
        }
    }

    private RiskEvaluationRequest toRequest(AccountFeatureSnapshot snapshot) {
        return new RiskEvaluationRequest(
                snapshot.accountId(),
                snapshot,
                graphRiskSignalResolver.resolve(snapshot),
                null,
                java.util.Map.of(),
                EvaluationMode.BATCH,
                snapshot.generatedAt());
    }

    private RiskEvaluationJob updateJob(
            RiskEvaluationJob current,
            RiskJobStatus status,
            int processed,
            int failed,
            String errorMessage,
            Instant finishedAt) {
        RiskEvaluationJob updated = new RiskEvaluationJob(
                current.jobId(),
                current.jobType(),
                current.startedAt(),
                finishedAt,
                status,
                current.targetAccountCount(),
                processed,
                failed,
                errorMessage);
        jobRepository.updateJobStatus(updated);
        return updated;
    }
}
