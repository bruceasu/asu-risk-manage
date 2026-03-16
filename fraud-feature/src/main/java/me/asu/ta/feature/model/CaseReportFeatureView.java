package me.asu.ta.feature.model;

public record CaseReportFeatureView(
        String accountId,
        int accountAgeDays,
        int highRiskIpLoginCount24h,
        int uniqueDeviceCount7d,
        boolean rapidWithdrawAfterDepositFlag24h,
        int sharedDeviceAccounts7d,
        int graphClusterSize30d,
        int riskNeighborCount30d
) {
}
