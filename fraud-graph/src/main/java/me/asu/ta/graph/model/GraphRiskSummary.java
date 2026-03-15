package me.asu.ta.graph.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Row model mapped to {@code graph_risk_summary}.
 *
 * @param clusterId primary key of the summarized cluster
 * @param clusterType cluster category used for downstream interpretation
 * @param clusterSize account count in the cluster
 * @param highRiskNodeCount number of accounts whose latest risk level is high or critical
 * @param sharedDeviceEdgeCount count of shared-device edges inside the cluster
 * @param sharedIpEdgeCount count of shared-ip edges inside the cluster
 * @param sharedBankEdgeCount count of shared-bank-account edges inside the cluster
 * @param transferEdgeCount count of transfer edges inside the cluster
 * @param collectorPresentFlag whether the cluster contains a collector-like account
 * @param clusterRiskScore cluster-level explicit risk score capped to 0-100
 * @param graphWindowStart inclusive graph build window start
 * @param graphWindowEnd exclusive graph build window end
 * @param generatedAt summary generation time
 */
public record GraphRiskSummary(
        String clusterId,
        GraphClusterType clusterType,
        int clusterSize,
        int highRiskNodeCount,
        Integer sharedDeviceEdgeCount,
        Integer sharedIpEdgeCount,
        Integer sharedBankEdgeCount,
        Integer transferEdgeCount,
        Boolean collectorPresentFlag,
        Double clusterRiskScore,
        Instant graphWindowStart,
        Instant graphWindowEnd,
        Instant generatedAt
) {
    public GraphRiskSummary {
        Objects.requireNonNull(clusterId, "clusterId");
        Objects.requireNonNull(clusterType, "clusterType");
        clusterSize = Math.max(0, clusterSize);
        highRiskNodeCount = Math.max(0, highRiskNodeCount);
        Objects.requireNonNull(graphWindowStart, "graphWindowStart");
        Objects.requireNonNull(graphWindowEnd, "graphWindowEnd");
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
