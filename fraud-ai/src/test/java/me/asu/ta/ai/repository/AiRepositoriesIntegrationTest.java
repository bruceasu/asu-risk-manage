package me.asu.ta.ai.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.AiGenerationRequestLog;
import me.asu.ta.ai.model.AiRequestStatus;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.ai.model.InvestigationReportStatus;
import org.junit.jupiter.api.Test;

class AiRepositoriesIntegrationTest {
    @Test
    void shouldPersistPromptTemplateAndLoadActiveVersion() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        PromptTemplateRepository repository = AiTestSupport.promptTemplateRepository(dataSource);
        repository.save(AiTestSupport.template("TMP", 1, me.asu.ta.ai.model.PromptTemplateType.SYSTEM, "old", true));
        repository.save(AiTestSupport.template("TMP", 2, me.asu.ta.ai.model.PromptTemplateType.SYSTEM, "new", true));

        assertEquals("new", repository.findActiveTemplate("TMP", me.asu.ta.ai.model.PromptTemplateType.SYSTEM).orElseThrow().templateContent());
    }

    @Test
    void shouldCreateAndUpdateRequestLog() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        AiGenerationRequestLogRepository repository = AiTestSupport.requestLogRepository(dataSource);
        AiGenerationRequestLog saved = repository.save(new AiGenerationRequestLog(
                null, 101L, "CASE", 1, "test-model", "{\"model\":\"test-model\"}", AiTestSupport.FIXED_TIME, AiRequestStatus.PENDING, null));

        repository.updateStatus(saved.requestId(), AiRequestStatus.SUCCESS, null);

        AiGenerationRequestLog loaded = repository.findByCaseId(101L).getFirst();
        assertEquals(AiRequestStatus.SUCCESS, loaded.status());
    }

    @Test
    void shouldPersistAndFindLatestReportByCaseId() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        InvestigationReportRepository repository = AiTestSupport.reportRepository(dataSource);
        repository.save(new InvestigationReport(
                null, 101L, InvestigationReportStatus.GENERATED, "title-1", "summary-1", "key-1",
                "behavior-1", "relationship-1", "timeline-1", "pattern-1", "rec-1",
                "test-model", "CASE", 1, AiTestSupport.FIXED_TIME.minusSeconds(60), "{}"));
        repository.save(new InvestigationReport(
                null, 101L, InvestigationReportStatus.GENERATED, "title-2", "summary-2", "key-2",
                "behavior-2", "relationship-2", "timeline-2", "pattern-2", "rec-2",
                "test-model", "CASE", 1, AiTestSupport.FIXED_TIME, "{}"));

        InvestigationReport latest = repository.findLatestByCaseId(101L).orElseThrow();
        assertEquals("title-2", latest.reportTitle());
        assertTrue(repository.findByCaseId(101L).size() >= 2);
    }
}
