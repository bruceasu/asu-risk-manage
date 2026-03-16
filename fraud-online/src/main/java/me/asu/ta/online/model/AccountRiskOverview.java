package me.asu.ta.online.model;

import java.util.List;

public record AccountRiskOverview(
        String accountId,
        RiskHistoryItem latestRisk,
        List<String> topReasonCodes,
        Object scoreBreakdown,
        Object latestFeature,
        List<RuleHitSummary> latestRuleHits,
        CaseSummaryView latestCase
) {
}
