package me.asu.ta.ai.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.LlmResponse;
import me.asu.ta.ai.model.RenderedPrompt;
import org.junit.jupiter.api.Test;

class InvestigationReportParserTest {
    @Test
    void shouldParseValidJsonResponse() {
        InvestigationReportParser parser = AiTestSupport.reportParser();

        var report = parser.parse(101L, new RenderedPrompt("INVESTIGATION_CASE_RENDERER", 1, "prompt"), AiTestSupport.successfulLlmResponse());

        assertEquals(101L, report.caseId());
        assertEquals("Critical takeover case", report.reportTitle());
        assertEquals("Freeze account and perform manual review", report.recommendations());
    }

    @Test
    void shouldUseAliasesAndDefaultEmptyStringsForMissingFields() {
        InvestigationReportParser parser = AiTestSupport.reportParser();
        LlmResponse response = new LlmResponse(
                "test-model",
                "{}",
                "{\"title\":\"Alias title\",\"summary\":\"Alias summary\",\"recommendedActions\":\"Do manual review\"}");

        var report = parser.parse(101L, new RenderedPrompt("INVESTIGATION_CASE_RENDERER", 1, "prompt"), response);

        assertEquals("Alias title", report.reportTitle());
        assertEquals("Alias summary", report.executiveSummary());
        assertEquals("Do manual review", report.recommendations());
        assertEquals("", report.keyRiskIndicators());
        assertEquals("", report.behaviorAnalysis());
    }

    @Test
    void shouldRejectNonObjectJson() {
        InvestigationReportParser parser = AiTestSupport.reportParser();
        LlmResponse response = new LlmResponse("test-model", "[]", "[]");

        assertThrows(InvestigationReportParseException.class,
                () -> parser.parse(101L, new RenderedPrompt("INVESTIGATION_CASE_RENDERER", 1, "prompt"), response));
    }
}
