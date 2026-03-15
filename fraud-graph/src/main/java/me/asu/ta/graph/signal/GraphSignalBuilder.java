package me.asu.ta.graph.signal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.model.AccountGraphSignal;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import me.asu.ta.graph.model.GraphRiskSummary;
import org.springframework.stereotype.Component;

@Component
public class GraphSignalBuilder {
    public List<AccountGraphSignal> buildSignals(GraphAnalysisSnapshot snapshot, Instant graphWindowStart, Instant graphWindowEnd) {
        Map<String, Set<String>> sharedDeviceAccounts = new HashMap<>();
        Map<String, Set<String>> sharedIpAccounts = new HashMap<>();
        Map<String, Set<String>> sharedBankAccounts = new HashMap<>();
        Map<String, Integer> totalNeighborCounts = new HashMap<>();
        Set<String> accounts = new HashSet<>();

        for (GraphEdge edge : snapshot.edges()) {
            accounts.add(edge.fromAccountId());
            accounts.add(edge.toAccountId());
            totalNeighborCounts.merge(edge.fromAccountId(), 1, Integer::sum);
            totalNeighborCounts.merge(edge.toAccountId(), 1, Integer::sum);
            track(edge, GraphEdgeType.SHARED_DEVICE, sharedDeviceAccounts);
            track(edge, GraphEdgeType.SHARED_IP, sharedIpAccounts);
            track(edge, GraphEdgeType.SHARED_BANK_ACCOUNT, sharedBankAccounts);
        }

        Map<String, Integer> clusterSizes = new HashMap<>();
        for (GraphClusterMembership membership : snapshot.clusters()) {
            accounts.add(membership.accountId());
            clusterSizes.put(membership.accountId(), membership.clusterSize());
        }

        Map<String, Double> clusterScoresByAccount = mapClusterScores(snapshot.clusters(), snapshot.graphRiskSummaries());
        Instant generatedAt = Instant.now();
        List<AccountGraphSignal> signals = new ArrayList<>();
        for (String accountId : accounts.stream().sorted().toList()) {
            int clusterSize = clusterSizes.getOrDefault(accountId, 1);
            int oneHop = snapshot.oneHopRiskNeighbors().getOrDefault(accountId, 0);
            int twoHop = snapshot.twoHopRiskNeighbors().getOrDefault(accountId, 0);
            int sharedDevice = sharedDeviceAccounts.getOrDefault(accountId, Set.of()).size();
            int sharedIp = sharedIpAccounts.getOrDefault(accountId, Set.of()).size();
            int sharedBank = sharedBankAccounts.getOrDefault(accountId, Set.of()).size();
            CollectorMetrics collectorMetrics = snapshot.collectorMetrics().getOrDefault(accountId, new CollectorMetrics(false, 0, 0));
            double localDensityScore = Math.min(
                    100.0d,
                    100.0d * totalNeighborCounts.getOrDefault(accountId, 0)
                            / Math.max(1.0d, clusterSize - 1.0d));
            double clusterRiskScore = clusterScoresByAccount.getOrDefault(accountId, 0.0d);
            double graphScore = Math.min(
                    100.0d,
                    0.25d * normalizeCount(clusterSize, 20)
                            + 0.25d * normalizeCount(oneHop, 10)
                            + 0.15d * normalizeCount(twoHop, 20)
                            + 0.15d * normalizeCount(sharedBank, 5)
                            + 0.10d * localDensityScore
                            + 0.10d * clusterRiskScore
                            + (collectorMetrics.collectorAccountFlag() ? 10.0d : 0.0d));

            signals.add(new AccountGraphSignal(
                    accountId,
                    graphWindowStart,
                    graphWindowEnd,
                    graphScore,
                    clusterSize,
                    oneHop,
                    twoHop,
                    sharedDevice,
                    sharedIp,
                    sharedBank,
                    collectorMetrics.collectorAccountFlag(),
                    collectorMetrics.funnelInDegree(),
                    collectorMetrics.funnelOutDegree(),
                    localDensityScore,
                    clusterRiskScore,
                    generatedAt));
        }
        return signals;
    }

    private void track(GraphEdge edge, GraphEdgeType edgeType, Map<String, Set<String>> store) {
        if (edge.edgeType() != edgeType) {
            return;
        }
        store.computeIfAbsent(edge.fromAccountId(), ignored -> new HashSet<>()).add(edge.toAccountId());
        store.computeIfAbsent(edge.toAccountId(), ignored -> new HashSet<>()).add(edge.fromAccountId());
    }

    private Map<String, Double> mapClusterScores(List<GraphClusterMembership> clusters, List<GraphRiskSummary> summaries) {
        Map<String, Double> scoreByClusterId = new HashMap<>();
        for (GraphRiskSummary summary : summaries) {
            scoreByClusterId.put(summary.clusterId(), summary.clusterRiskScore() == null ? 0.0d : summary.clusterRiskScore());
        }
        Map<String, Double> scoreByAccountId = new HashMap<>();
        for (GraphClusterMembership membership : clusters) {
            scoreByAccountId.put(membership.accountId(), scoreByClusterId.getOrDefault(membership.clusterId(), 0.0d));
        }
        return scoreByAccountId;
    }

    private double normalizeCount(int count, int cap) {
        return 100.0d * Math.min(count, cap) / Math.max(cap, 1);
    }
}
