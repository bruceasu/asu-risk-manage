package me.asu.ta.feature.job;

import java.time.Instant;
import java.util.List;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.model.FeatureGenerationJob;
import me.asu.ta.feature.repository.AccountBatchReader;
import me.asu.ta.feature.repository.FeatureGenerationJobRepository;
import me.asu.ta.feature.service.FeatureGenerationService;
import me.asu.ta.feature.service.FeaturePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeatureGenerationJobRunner {
    private static final Logger log = LoggerFactory.getLogger(FeatureGenerationJobRunner.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_FEATURE_VERSION = 1;
    private static final int MAX_RETRIES = 3;

    private final FeatureGenerationJobRepository jobRepository;
    private final AccountBatchReader accountBatchReader;
    private final FeatureGenerationService featureGenerationService;
    private final FeaturePersistenceService featurePersistenceService;

    public FeatureGenerationJobRunner(
            FeatureGenerationJobRepository jobRepository,
            AccountBatchReader accountBatchReader,
            FeatureGenerationService featureGenerationService,
            FeaturePersistenceService featurePersistenceService) {
        this.jobRepository = jobRepository;
        this.accountBatchReader = accountBatchReader;
        this.featureGenerationService = featureGenerationService;
        this.featurePersistenceService = featurePersistenceService;
    }

    public FeatureGenerationJob runFullGeneration() {
        Instant startedAt = Instant.now();
        int targetAccountCount = accountBatchReader.countAccounts();
        FeatureGenerationJob startedJob = new FeatureGenerationJob(
                0L,
                "FULL_ACCOUNT_FEATURE_GENERATION",
                DEFAULT_FEATURE_VERSION,
                startedAt,
                null,
                "RUNNING",
                targetAccountCount,
                0,
                0,
                null);
        long jobId = jobRepository.saveAndReturnId(startedJob);
        log.info("Started feature generation job {}, targetAccountCount={}, batchSize={}", jobId, targetAccountCount, DEFAULT_BATCH_SIZE);

        int processedAccounts = 0;
        int failedAccounts = 0;
        String lastAccountId = null;

        try {
            while (true) {
                List<String> accountIds = accountBatchReader.fetchNextBatch(lastAccountId, DEFAULT_BATCH_SIZE);
                if (accountIds.isEmpty()) {
                    break;
                }
                lastAccountId = accountIds.getLast();
                boolean batchSucceeded = false;
                RuntimeException lastFailure = null;

                for (int attempt = 1; attempt <= MAX_RETRIES && !batchSucceeded; attempt++) {
                    try {
                        processBatch(jobId, accountIds);
                        processedAccounts += accountIds.size();
                        batchSucceeded = true;
                        updateProgress(jobId, startedAt, targetAccountCount, processedAccounts, failedAccounts, "RUNNING", null);
                        log.info(
                                "Feature generation job {} processed batch ending at accountId={}, batchSize={}, processed={}, failed={}",
                                jobId,
                                lastAccountId,
                                accountIds.size(),
                                processedAccounts,
                                failedAccounts);
                    } catch (RuntimeException ex) {
                        lastFailure = ex;
                        log.warn(
                                "Feature generation job {} failed batch ending at accountId={} on attempt {}/{}: {}",
                                jobId,
                                lastAccountId,
                                attempt,
                                MAX_RETRIES,
                                ex.getMessage());
                    }
                }

                if (!batchSucceeded) {
                    failedAccounts += accountIds.size();
                    String errorMessage = lastFailure == null ? "Unknown batch failure" : lastFailure.getMessage();
                    updateProgress(jobId, startedAt, targetAccountCount, processedAccounts, failedAccounts, "FAILED", errorMessage);
                    throw new IllegalStateException("Feature generation job failed after retries, jobId=" + jobId, lastFailure);
                }
            }

            FeatureGenerationJob completedJob = updateProgress(
                    jobId,
                    startedAt,
                    targetAccountCount,
                    processedAccounts,
                    failedAccounts,
                    "COMPLETED",
                    null);
            log.info(
                    "Completed feature generation job {}, processed={}, failed={}, target={}",
                    jobId,
                    processedAccounts,
                    failedAccounts,
                    targetAccountCount);
            return completedJob;
        } catch (RuntimeException ex) {
            updateProgress(
                    jobId,
                    startedAt,
                    targetAccountCount,
                    processedAccounts,
                    failedAccounts,
                    "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }

    private void processBatch(long jobId, List<String> accountIds) {
        List<AccountFeatureSnapshot> snapshots = featureGenerationService.generateFeaturesBatch(accountIds);
        featurePersistenceService.persistBatch(snapshots);
        log.debug("Feature generation job {} persisted {} snapshots and histories", jobId, snapshots.size());
    }

    private FeatureGenerationJob updateProgress(
            long jobId,
            Instant startedAt,
            int targetAccountCount,
            int processedAccounts,
            int failedAccounts,
            String status,
            String errorMessage) {
        FeatureGenerationJob job = new FeatureGenerationJob(
                jobId,
                "FULL_ACCOUNT_FEATURE_GENERATION",
                DEFAULT_FEATURE_VERSION,
                startedAt,
                "RUNNING".equals(status) ? null : Instant.now(),
                status,
                targetAccountCount,
                processedAccounts,
                failedAccounts,
                errorMessage);
        jobRepository.update(job);
        return job;
    }
}
