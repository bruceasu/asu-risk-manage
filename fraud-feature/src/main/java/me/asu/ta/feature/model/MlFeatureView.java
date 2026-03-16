package me.asu.ta.feature.model;

public record MlFeatureView(
        String accountId,
        int loginCount24h,
        int uniqueIpCount24h,
        int uniqueDeviceCount7d,
        int transactionCount24h,
        double totalAmount24h,
        double avgTransactionAmount24h,
        double depositWithdrawRatio24h,
        int deviceSwitchCount24h,
        int securityEventCount24h,
        int graphClusterSize30d
) {
}
