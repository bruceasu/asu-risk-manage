package me.asu.ta.graph.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import me.asu.ta.graph.model.GraphRiskSummary;
import org.springframework.stereotype.Component;

@Component
public class ClusterRiskScorer {
    public List<GraphRiskSummary> buildSummaries(
            List<GraphClusterMembership> clusters,
            List<GraphEdge> edges,
            Set<String> riskyAccounts,
            Map<String, CollectorMetrics> collectorMetrics,
            Instant graphWindowStart,
            Instant graphWindowEnd) {
        Map<String, List<GraphClusterMembership>> membershipsByCluster = new HashMap<>();
        for (GraphClusterMembership membership : clusters) {
            membershipsByCluster.computeIfAbsent(membership.clusterId(), ignored -> new ArrayList<>()).add(membership);
        }

        Instant generatedAt = Instant.now();
        List<GraphRiskSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<GraphClusterMembership>> entry : membershipsByCluster.entrySet()) {
            String clusterId = entry.getKey();
            List<GraphClusterMembership> memberships = entry.getValue();
            Set<String> accounts = new HashSet<>();
            for (GraphClusterMembership membership : memberships) {
                accounts.add(membership.accountId());
            }

            int sharedDeviceEdgeCount = 0;
            int sharedIpEdgeCount = 0;
            int sharedBankEdgeCount = 0;
            int transferEdgeCount = 0;
            for (GraphEdge edge : edges) {
                if (!accounts.contains(edge.fromAccountId()) || !accounts.contains(edge.toAccountId())) {
                    continue;
                }
                if (edge.edgeType() == GraphEdgeType.SHARED_DEVICE) {
                    sharedDeviceEdgeCount++;
                } else if (edge.edgeType() == GraphEdgeType.SHARED_IP) {
                    sharedIpEdgeCount++;
                } else if (edge.edgeType() == GraphEdgeType.SHARED_BANK_ACCOUNT) {
                    sharedBankEdgeCount++;
                } else if (edge.edgeType() == GraphEdgeType.TRANSFER) {
                    transferEdgeCount++;
                }
            }

            int highRiskNodeCount = 0;
            boolean collectorPresent = false;
            for (String accountId : accounts) {
                if (riskyAccounts.contains(accountId)) {
                    highRiskNodeCount++;
                }
                if (collectorMetrics.getOrDefault(accountId, new CollectorMetrics(false, 0, 0)).collectorAccountFlag()) {
                    collectorPresent = true;
                }
            }

            int clusterSize = memberships.isEmpty() ? 0 : memberships.getFirst().clusterSize();
            double maxUndirectedEdges = Math.max(1.0d, clusterSize * Math.max(clusterSize - 1, 1) / 2.0d);
            double highRiskRatio = clusterSize == 0 ? 0.0d : (double) highRiskNodeCount / clusterSize;
            double sharedBankDensity = sharedBankEdgeCount / maxUndirectedEdges;
            double sharedDeviceDensity = sharedDeviceEdgeCount / maxUndirectedEdges;
            double collectorPresenceScore = collectorPresent ? 1.0d : 0.0d;
            double clusterRiskScore = Math.min(
                    100.0d,
                    100.0d * (
                            0.35d * highRiskRatio
                                    + 0.25d * sharedBankDensity
                                    + 0.20d * sharedDeviceDensity
                                    + 0.20d * collectorPresenceScore));

            GraphClusterMembership first = memberships.getFirst();
            summaries.add(new GraphRiskSummary(
                    clusterId,
                    first.clusterType(),
                    clusterSize,
                    highRiskNodeCount,
                    sharedDeviceEdgeCount,
                    sharedIpEdgeCount,
                    sharedBankEdgeCount,
                    transferEdgeCount,
                    collectorPresent,
                    clusterRiskScore,
                    graphWindowStart,
                    graphWindowEnd,
                    generatedAt));
        }
        return summaries;
    }
}
