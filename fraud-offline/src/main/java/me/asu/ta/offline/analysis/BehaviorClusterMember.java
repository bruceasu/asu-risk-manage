package me.asu.ta.offline.analysis;

public record BehaviorClusterMember(
        int clusterId,
        String accountId,
        int clusterSize,
        double vectorNorm,
        String note) {
}
