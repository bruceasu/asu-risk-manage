package me.asu.ta.online.model;

import java.time.Instant;

public record FeatureHistoryItem(
        long snapshotId,
        Instant snapshotTime,
        int featureVersion,
        Integer transactionCount24h,
        Double totalAmount24h,
        Integer sharedDeviceAccounts7d,
        Integer graphClusterSize30d,
        Integer riskNeighborCount30d
) {
}
