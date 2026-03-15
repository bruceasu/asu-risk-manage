package me.asu.ta.ai.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.ai.model.InvestigationReportStatus;
import me.asu.ta.ai.model.LlmResponse;
import me.asu.ta.ai.model.RenderedPrompt;
import org.springframework.stereotype.Component;

@Component
public class InvestigationReportParser {
    private final ObjectMapper objectMapper;

    public InvestigationReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InvestigationReport parse(long caseId, RenderedPrompt casePrompt, LlmResponse llmResponse) {
        try {
            JsonNode root = objectMapper.readTree(llmResponse.content());
            if (!root.isObject()) {
                throw new InvestigationReportParseException("LLM content must be a JSON object");
            }
            return new InvestigationReport(
                    null,
                    caseId,
                    InvestigationReportStatus.GENERATED,
                    textOrEmpty(root, "reportTitle", "title", "report_title"),
                    textOrEmpty(root, "executiveSummary", "executive_summary", "summary"),
                    textOrEmpty(root, "keyRiskIndicators", "key_risk_indicators", "riskIndicators", "risk_indicators"),
                    textOrEmpty(root, "behaviorAnalysis", "behavior_analysis"),
                    textOrEmpty(root, "relationshipAnalysis", "relationship_analysis"),
                    textOrEmpty(root, "timelineObservations", "timeline_observations", "timelineAnalysis"),
                    textOrEmpty(root, "possibleRiskPatterns", "possible_risk_patterns", "riskPatterns", "risk_patterns"),
                    textOrEmpty(root, "recommendations", "recommendedActions", "recommended_actions"),
                    llmResponse.modelName(),
                    casePrompt.templateCode(),
                    casePrompt.templateVersion(),
                    Instant.now(),
                    llmResponse.rawResponse());
        } catch (JsonProcessingException ex) {
            throw new InvestigationReportParseException("Failed to parse LLM response", ex);
        }
    }

    private String textOrEmpty(JsonNode root, String... aliases) {
        for (String alias : aliases) {
            JsonNode node = root.path(alias);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            return node.isTextual() ? node.asText() : node.toString();
        }
        return "";
    }
}
