package me.asu.ta.ai.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.ai.model.AiGenerationJob;
import me.asu.ta.ai.model.AiJobStatus;
import me.asu.ta.ai.repository.AiGenerationJobRepository;
import me.asu.ta.ai.repository.InvestigationCaseBatchReader;
import me.asu.ta.ai.service.AiReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiGenerationJobRunner {
    private static final Logger log = LoggerFactory.getLogger(AiGenerationJobRunner.class);
    private static final int DEFAULT_BATCH_SIZE = 500;
    private final InvestigationCaseBatchReader caseBatchReader;
    private final AiGenerationJobRepository jobRepository;
    private final AiReportService aiReportService;

    public AiGenerationJobRunner(
            InvestigationCaseBatchReader caseBatchReader,
            AiGenerationJobRepository jobRepository,
            AiReportService aiReportService) {
        this.caseBatchReader = caseBatchReader;
        this.jobRepository = jobRepository;
        this.aiReportService = aiReportService;
    }

    public AiGenerationJob runFullGeneration() {
        AiGenerationJob job = jobRepository.createJob(new AiGenerationJob(
                null,
                "BATCH_REPORT",
                Instant.now(),
                null,
                AiJobStatus.RUNNING,
                caseBatchReader.countCases(),
                0,
                0,
                null));

        Long lastCaseId = null;
        List<String> errors = new ArrayList<>();
        try {
            while (true) {
                List<Long> batch = caseBatchReader.nextBatch(lastCaseId, DEFAULT_BATCH_SIZE);
                if (batch.isEmpty()) {
                    break;
                }
                log.info("Processing fraud-ai batch, jobId={}, batchSize={}, lastCaseId={}",
                        job.jobId(), batch.size(), lastCaseId);
                for (Long caseId : batch) {
                    try {
                        aiReportService.generateReport(caseId);
                        jobRepository.incrementProcessedCount(job.jobId());
                    } catch (RuntimeException ex) {
                        jobRepository.incrementFailedCount(job.jobId());
                        if (errors.size() < 10) {
                            errors.add("caseId=" + caseId + ":" + ex.getMessage());
                        }
                        log.error("AI report generation failed for caseId={}, jobId={}", caseId, job.jobId(), ex);
                    }
                    lastCaseId = caseId;
                }
            }
            AiGenerationJob finalJob = jobRepository.findById(job.jobId());
            AiJobStatus finalStatus = finalJob.failedCaseCount() != null && finalJob.failedCaseCount() > 0
                    ? AiJobStatus.PARTIAL_SUCCESS
                    : AiJobStatus.COMPLETED;
            jobRepository.updateJobStatus(job.jobId(), finalStatus, Instant.now(), errors.isEmpty() ? null : String.join("\n", errors));
            return jobRepository.findById(job.jobId());
        } catch (RuntimeException ex) {
            jobRepository.updateJobStatus(job.jobId(), AiJobStatus.FAILED, Instant.now(), ex.getMessage());
            throw ex;
        }
    }
}
