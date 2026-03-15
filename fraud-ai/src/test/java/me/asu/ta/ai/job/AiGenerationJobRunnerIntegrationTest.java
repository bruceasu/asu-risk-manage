package me.asu.ta.ai.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;
import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.AiGenerationJob;
import me.asu.ta.ai.model.AiJobStatus;
import me.asu.ta.ai.service.AiReportService;
import org.junit.jupiter.api.Test;

class AiGenerationJobRunnerIntegrationTest {
    @Test
    void shouldContinueProcessingCasesWhenSingleCaseFails() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        AiTestSupport.insertInvestigationCaseId(dataSource, 1L);
        AiTestSupport.insertInvestigationCaseId(dataSource, 2L);
        AiTestSupport.insertInvestigationCaseId(dataSource, 3L);

        AiReportService service = new AiReportService(
                null, null, null, null, null, null, null, null,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource),
                AiTestSupport.objectMapper()) {
            @Override
            public me.asu.ta.ai.model.InvestigationReport generateReport(long caseId) {
                if (caseId == 2L) {
                    throw new IllegalStateException("Synthetic ai generation failure");
                }
                return new me.asu.ta.ai.model.InvestigationReport(
                        null, caseId, me.asu.ta.ai.model.InvestigationReportStatus.GENERATED,
                        "title-" + caseId, "summary", "key", "behavior", "relationship",
                        "timeline", "patterns", "recommendations", "test-model", "CASE", 1,
                        AiTestSupport.FIXED_TIME, "{}");
            }
        };

        AiGenerationJobRunner runner = new AiGenerationJobRunner(
                AiTestSupport.caseBatchReader(dataSource),
                AiTestSupport.jobRepository(dataSource),
                service);

        AiGenerationJob job = runner.runFullGeneration();

        assertEquals(AiJobStatus.PARTIAL_SUCCESS, job.status());
        assertEquals(2, job.processedCaseCount());
        assertEquals(1, job.failedCaseCount());
    }
}
