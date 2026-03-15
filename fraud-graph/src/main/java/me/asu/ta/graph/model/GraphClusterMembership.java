package me.asu.ta.graph.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Row model mapped to {@code account_graph_cluster}.
 *
 * @param clusterId stable cluster identifier within a graph window
 * @param accountId member account id contained in the cluster
 * @param clusterType dominant cluster relationship type
 * @param clusterSize number of accounts in the cluster
 * @param graphWindowStart inclusive graph build window start
 * @param graphWindowEnd exclusive graph build window end
 * @param createdAt persistence creation time for this membership row
 */
public record GraphClusterMembership(
        String clusterId,
        String accountId,
        GraphClusterType clusterType,
        int clusterSize,
        Instant graphWindowStart,
        Instant graphWindowEnd,
        Instant createdAt
) {
    public GraphClusterMembership {
        Objects.requireNonNull(clusterId, "clusterId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(clusterType, "clusterType");
        clusterSize = Math.max(0, clusterSize);
        Objects.requireNonNull(graphWindowStart, "graphWindowStart");
        Objects.requireNonNull(graphWindowEnd, "graphWindowEnd");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
