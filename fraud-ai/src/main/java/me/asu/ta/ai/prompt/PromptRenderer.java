package me.asu.ta.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.ai.model.PromptTemplate;
import me.asu.ta.ai.model.RenderedPrompt;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import org.springframework.stereotype.Component;

@Component
public class PromptRenderer {
    private final ObjectMapper objectMapper;

    public PromptRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderedPrompt renderStaticPrompt(PromptTemplate template) {
        return new RenderedPrompt(template.templateCode(), template.version(), template.templateContent());
    }

    public RenderedPrompt renderCasePrompt(PromptTemplate template, InvestigationCaseBundle bundle) {
        String rendered = template.templateContent()
                .replace("{{caseId}}", String.valueOf(bundle.investigationCase().caseId()))
                .replace("{{accountId}}", bundle.investigationCase().accountId())
                .replace("{{riskLevel}}", bundle.investigationCase().riskLevel().name())
                .replace("{{riskScore}}", String.valueOf(bundle.investigationCase().riskScore()))
                .replace("{{topReasonCodes}}", join(bundle.investigationCase().topReasonCodes()))
                .replace("{{riskSummary}}", riskSummaryJson(bundle))
                .replace("{{featureSummary}}", featureSummaryJson(bundle))
                .replace("{{ruleHits}}", ruleHitsJson(bundle.ruleHits()))
                .replace("{{graphSummary}}", graphSummaryJson(bundle))
                .replace("{{timeline}}", timelineJson(bundle.timelineEvents()))
                .replace("{{recommendedActions}}", actionsJson(bundle.recommendedActions()));
        return new RenderedPrompt(template.templateCode(), template.version(), rendered);
    }

    public String combineUserPrompt(RenderedPrompt reportFormatPrompt, RenderedPrompt casePrompt) {
        return "OUTPUT_FORMAT:\n" + reportFormatPrompt.renderedContent()
                + "\n\nCASE_INPUT:\n" + casePrompt.renderedContent();
    }

    private String riskSummaryJson(InvestigationCaseBundle bundle) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("riskScore", bundle.investigationCase().riskScore());
        payload.put("riskLevel", bundle.investigationCase().riskLevel().name());
        payload.put("scoreBreakdownJson", bundle.riskSummary().scoreBreakdownJson());
        payload.put("graphScore", bundle.riskSummary().graphScore());
        payload.put("behaviorScore", bundle.riskSummary().behaviorScore());
        return write(payload);
    }

    private String featureSummaryJson(InvestigationCaseBundle bundle) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountAgeDays", bundle.featureSummary().accountAgeDays());
        payload.put("highRiskIpLoginCount24h", bundle.featureSummary().highRiskIpLoginCount24h());
        payload.put("loginFailureRate24h", bundle.featureSummary().loginFailureRate24h());
        payload.put("newDeviceLoginCount7d", bundle.featureSummary().newDeviceLoginCount7d());
        payload.put("sharedDeviceAccounts7d", bundle.featureSummary().sharedDeviceAccounts7d());
        payload.put("graphClusterSize30d", bundle.featureSummary().graphClusterSize30d());
        return write(payload);
    }

    private String ruleHitsJson(List<CaseRuleHit> ruleHits) {
        return write(ruleHits.stream().map(ruleHit -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ruleCode", ruleHit.ruleCode());
            payload.put("severity", ruleHit.severity().name());
            payload.put("score", ruleHit.score());
            payload.put("reasonCode", ruleHit.reasonCode());
            payload.put("message", ruleHit.message());
            return payload;
        }).toList());
    }

    private String graphSummaryJson(InvestigationCaseBundle bundle) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("graphScore", bundle.graphSummary().graphScore());
        payload.put("graphClusterSize", bundle.graphSummary().graphClusterSize());
        payload.put("riskNeighborCount", bundle.graphSummary().riskNeighborCount());
        payload.put("sharedDeviceAccounts", bundle.graphSummary().sharedDeviceAccounts());
        payload.put("sharedBankAccounts", bundle.graphSummary().sharedBankAccounts());
        return write(payload);
    }

    private String timelineJson(List<CaseTimelineEvent> timelineEvents) {
        return write(timelineEvents.stream().map(event -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventTime", event.eventTime().toString());
            payload.put("eventType", event.eventType());
            payload.put("title", event.title());
            payload.put("description", event.description());
            return payload;
        }).toList());
    }

    private String actionsJson(List<CaseRecommendedAction> actions) {
        return write(actions.stream().map(action -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("actionCode", action.actionCode());
            payload.put("actionReason", action.actionReason());
            return payload;
        }).toList());
    }

    private String write(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render prompt payload", ex);
        }
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
    }
}
