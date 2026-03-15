package me.asu.ta.feature.model;

public record RiskEngineFeatureView(
        String accountId,
        int loginFailureCount24h,
        double loginFailureRate24h,
        int highRiskIpLoginCount24h,
        double depositWithdrawRatio24h,
        boolean rapidWithdrawAfterDepositFlag24h,
        int sharedDeviceAccounts7d,
        boolean securityChangeBeforeWithdrawFlag24h,
        int graphClusterSize30d,
        int riskNeighborCount30d,
        double anomalyScoreLast
) {
}
