package me.asu.ta.casemanagement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import me.asu.ta.casemanagement.CaseTestSupport;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseRetrievalService;
import org.junit.jupiter.api.Test;

class CasePersistenceIntegrationTest {
    @Test
    void shouldPersistAndRetrieveCaseSummaryAndDetailTables() throws Exception {
        DataSource dataSource = CaseTestSupport.createDataSource();

        InvestigationCaseBundle bundle = CaseTestSupport.persistCaseBundle(dataSource, "acct-case-1");
        CaseRetrievalService retrievalService = CaseTestSupport.caseRetrievalService(dataSource);

        InvestigationCaseBundle loaded = retrievalService.getCaseById(bundle.investigationCase().caseId()).orElseThrow();

        assertEquals("acct-case-1", loaded.investigationCase().accountId());
        assertEquals(2, loaded.ruleHits().size());
        assertTrue(loaded.riskSummary().scoreBreakdownJson().contains("\"finalScore\":91.0"));
        assertTrue(loaded.timelineEvents().stream().anyMatch(event -> "CASE_CREATED".equals(event.eventType())));
        assertTrue(loaded.recommendedActions().stream().anyMatch(action -> "FREEZE_ACCOUNT".equals(action.actionCode())));
    }

    @Test
    void shouldRetrieveLatestCaseByAccountId() throws Exception {
        DataSource dataSource = CaseTestSupport.createDataSource();

        InvestigationCaseBundle first = CaseTestSupport.persistCaseBundle(dataSource, "acct-latest");
        InvestigationCaseBundle second = CaseTestSupport.persistCaseBundle(dataSource, "acct-latest");

        CaseRetrievalService retrievalService = CaseTestSupport.caseRetrievalService(dataSource);
        InvestigationCaseBundle latest = retrievalService.getLatestCaseByAccountId("acct-latest").orElseThrow();

        assertTrue(second.investigationCase().caseId() > first.investigationCase().caseId());
        assertEquals(second.investigationCase().caseId(), latest.investigationCase().caseId());
    }
}
