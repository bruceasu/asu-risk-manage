package me.asu.ta.casemanagement.model;

import java.util.List;
import java.util.Objects;

/**
 * Structured aggregate returned to internal modules.
 */
public record InvestigationCaseBundle(
        InvestigationCase investigationCase,
        CaseRiskSummary riskSummary,
        CaseFeatureSummary featureSummary,
        List<CaseRuleHit> ruleHits,
        CaseGraphSummary graphSummary,
        List<CaseTimelineEvent> timelineEvents,
        List<CaseRecommendedAction> recommendedActions
) {
    public InvestigationCaseBundle {
        Objects.requireNonNull(investigationCase, "investigationCase");
        Objects.requireNonNull(riskSummary, "riskSummary");
        Objects.requireNonNull(featureSummary, "featureSummary");
        Objects.requireNonNull(graphSummary, "graphSummary");
        ruleHits = ruleHits == null ? List.of() : List.copyOf(ruleHits);
        timelineEvents = timelineEvents == null ? List.of() : List.copyOf(timelineEvents);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
