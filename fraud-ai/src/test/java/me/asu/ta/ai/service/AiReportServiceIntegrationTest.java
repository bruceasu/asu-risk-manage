package me.asu.ta.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.client.LlmClient;
import me.asu.ta.ai.model.LlmRequest;
import me.asu.ta.ai.model.LlmResponse;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseFacade;
import org.junit.jupiter.api.Test;

class AiReportServiceIntegrationTest {
    @Test
    void shouldGenerateSingleCaseReportAndPersistAuditRecords() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        AiTestSupport.insertDefaultTemplates(dataSource);
        InvestigationCaseBundle bundle = AiTestSupport.sampleCaseBundle(101L, "acct-ai-1");
        CaseFacade caseFacade = AiTestSupport.caseFacadeReturning(bundle);
        LlmClient llmClient = new LlmClient() {
            @Override
            public LlmResponse generate(LlmRequest request) {
                return AiTestSupport.successfulLlmResponse();
            }
        };

        AiReportService service = AiTestSupport.aiReportService(dataSource, caseFacade, llmClient);
        var report = service.generateReport(101L);

        assertEquals(101L, report.caseId());
        assertEquals("Critical takeover case", report.reportTitle());
        assertEquals(1, AiTestSupport.requestLogRepository(dataSource).findByCaseId(101L).size());
        assertEquals(1, AiTestSupport.reportRepository(dataSource).findByCaseId(101L).size());
        assertTrue(AiTestSupport.requestLogRepository(dataSource).findByCaseId(101L).getFirst().requestPayload().contains("test-model"));
    }
}
