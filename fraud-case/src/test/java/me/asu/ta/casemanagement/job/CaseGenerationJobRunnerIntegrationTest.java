package me.asu.ta.casemanagement.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;
import me.asu.ta.casemanagement.CaseTestSupport;
import me.asu.ta.casemanagement.model.CaseGenerationJob;
import me.asu.ta.casemanagement.model.CaseJobStatus;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.repository.CaseAccountBatchReader;
import me.asu.ta.casemanagement.repository.CaseGenerationJobRepository;
import me.asu.ta.casemanagement.service.CaseService;
import org.junit.jupiter.api.Test;

class CaseGenerationJobRunnerIntegrationTest {
    @Test
    void shouldContinueProcessingAccountsWhenSingleAccountFails() throws Exception {
        DataSource dataSource = CaseTestSupport.createDataSource();
        CaseTestSupport.insertAccountSnapshot(dataSource, "acct-batch-1");
        CaseTestSupport.insertAccountSnapshot(dataSource, "acct-fail");
        CaseTestSupport.insertAccountSnapshot(dataSource, "acct-batch-2");

        CaseGenerationJobRepository jobRepository = CaseTestSupport.caseGenerationJobRepository(dataSource);
        CaseAccountBatchReader batchReader = new CaseAccountBatchReader(CaseTestSupport.jdbcTemplate(dataSource));
        CaseService fakeCaseService = new CaseService(null, null) {
            @Override
            public InvestigationCaseBundle createCase(String accountId) {
                if ("acct-fail".equals(accountId)) {
                    throw new IllegalStateException("Synthetic case build failure");
                }
                return null;
            }
        };

        CaseGenerationJobRunner runner = new CaseGenerationJobRunner(batchReader, jobRepository, fakeCaseService);
        CaseGenerationJob job = runner.runFullGeneration();

        assertEquals(CaseJobStatus.PARTIAL_SUCCESS, job.status());
        assertEquals(3, job.targetAccountCount());
        assertEquals(2, job.processedAccountCount());
        assertEquals(1, job.failedAccountCount());
        assertEquals(job.jobId(), jobRepository.findById(job.jobId()).orElseThrow().jobId());
    }
}
