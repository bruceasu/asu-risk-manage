package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Feature summary mapped to {@code case_feature_summary}.
 */
public record CaseFeatureSummary(
        long caseId,
        Integer accountAgeDays,
        Integer highRiskIpLoginCount24h,
        Double loginFailureRate24h,
        Integer newDeviceLoginCount7d,
        Double withdrawAfterDepositDelayAvg24h,
        Integer sharedDeviceAccounts7d,
        Boolean securityChangeBeforeWithdrawFlag24h,
        Integer graphClusterSize30d,
        Integer riskNeighborCount30d,
        Double anomalyScoreLast,
        Instant createdAt
) {
    public CaseFeatureSummary {
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
