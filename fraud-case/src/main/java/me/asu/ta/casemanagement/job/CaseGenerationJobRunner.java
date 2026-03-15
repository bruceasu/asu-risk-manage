package me.asu.ta.casemanagement.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseGenerationJob;
import me.asu.ta.casemanagement.model.CaseJobStatus;
import me.asu.ta.casemanagement.repository.CaseAccountBatchReader;
import me.asu.ta.casemanagement.repository.CaseGenerationJobRepository;
import me.asu.ta.casemanagement.service.CaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CaseGenerationJobRunner {
    private static final Logger log = LoggerFactory.getLogger(CaseGenerationJobRunner.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_ERROR_DETAILS = 10;

    private final CaseAccountBatchReader accountBatchReader;
    private final CaseGenerationJobRepository jobRepository;
    private final CaseService caseService;

    public CaseGenerationJobRunner(
            CaseAccountBatchReader accountBatchReader,
            CaseGenerationJobRepository jobRepository,
            CaseService caseService) {
        this.accountBatchReader = accountBatchReader;
        this.jobRepository = jobRepository;
        this.caseService = caseService;
    }

    public CaseGenerationJob runFullGeneration() {
        int targetCount = accountBatchReader.countAccounts();
        CaseGenerationJob job = jobRepository.createJob(new CaseGenerationJob(
                0L,
                "FULL_CASE_GENERATION",
                Instant.now(),
                null,
                CaseJobStatus.RUNNING,
                targetCount,
                0,
                0,
                null));

        int processed = 0;
        int failed = 0;
        String lastAccountId = null;
        List<String> errorDetails = new ArrayList<>();
        try {
            while (true) {
                List<String> batch = accountBatchReader.nextBatch(lastAccountId, DEFAULT_BATCH_SIZE);
                if (batch.isEmpty()) {
                    break;
                }
                log.info("Processing fraud-case batch, jobId={}, batchSize={}, lastAccountId={}", job.jobId(), batch.size(), lastAccountId);
                for (String accountId : batch) {
                    try {
                        caseService.createCase(accountId);
                        processed++;
                    } catch (RuntimeException ex) {
                        failed++;
                        log.error("Fraud-case generation failed for accountId={}, jobId={}", accountId, job.jobId(), ex);
                        if (errorDetails.size() < MAX_ERROR_DETAILS) {
                            errorDetails.add(accountId + ": " + compactMessage(ex));
                        }
                    }
                }
                lastAccountId = batch.getLast();
                job = new CaseGenerationJob(
                        job.jobId(),
                        job.jobType(),
                        job.startedAt(),
                        null,
                        CaseJobStatus.RUNNING,
                        targetCount,
                        processed,
                        failed,
                        summarizeErrors(errorDetails));
                jobRepository.updateJobStatus(job);
            }

            CaseJobStatus finalStatus = failed > 0 ? CaseJobStatus.PARTIAL_SUCCESS : CaseJobStatus.COMPLETED;
            job = new CaseGenerationJob(
                    job.jobId(),
                    job.jobType(),
                    job.startedAt(),
                    Instant.now(),
                    finalStatus,
                    targetCount,
                    processed,
                    failed,
                    summarizeErrors(errorDetails));
            jobRepository.updateJobStatus(job);
            return job;
        } catch (RuntimeException ex) {
            log.error("Fraud-case generation job failed, jobId={}", job.jobId(), ex);
            job = new CaseGenerationJob(
                    job.jobId(),
                    job.jobType(),
                    job.startedAt(),
                    Instant.now(),
                    CaseJobStatus.FAILED,
                    targetCount,
                    processed,
                    failed,
                    summarizeErrors(errorDetails.isEmpty() ? List.of(compactMessage(ex)) : errorDetails));
            jobRepository.updateJobStatus(job);
            throw ex;
        }
    }

    private String summarizeErrors(List<String> errorDetails) {
        if (errorDetails == null || errorDetails.isEmpty()) {
            return null;
        }
        return String.join(" | ", errorDetails);
    }

    private String compactMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}
