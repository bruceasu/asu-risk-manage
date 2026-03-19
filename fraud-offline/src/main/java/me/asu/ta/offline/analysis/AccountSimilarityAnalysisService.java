package me.asu.ta.offline.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.ClusterHelper;

public final class AccountSimilarityAnalysisService {
    public List<BehaviorSimilarityEdge> analyze(
            Map<String, AccountBehaviorFeatureVector> features,
            double threshold,
            int topNPerAccount) {
        if (features == null || features.size() < 2) {
            return List.of();
        }
        List<AccountBehaviorFeatureVector> vectors = new ArrayList<>(features.values());
        Map<String, List<NeighborCandidate>> candidates = new HashMap<>();
        for (int i = 0; i < vectors.size(); i++) {
            for (int j = i + 1; j < vectors.size(); j++) {
                AccountBehaviorFeatureVector left = vectors.get(i);
                AccountBehaviorFeatureVector right = vectors.get(j);
                double similarity = ClusterHelper.cosine(left.getNormalizedVector(), right.getNormalizedVector());
                if (similarity < threshold) {
                    continue;
                }
                candidates.computeIfAbsent(left.getAccountId(), key -> new ArrayList<>())
                        .add(new NeighborCandidate(right.getAccountId(), similarity));
                candidates.computeIfAbsent(right.getAccountId(), key -> new ArrayList<>())
                        .add(new NeighborCandidate(left.getAccountId(), similarity));
            }
        }
        Map<String, Map<String, Integer>> topRanks = buildTopRanks(candidates, topNPerAccount);
        Map<String, BehaviorSimilarityEdge> edges = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : topRanks.entrySet()) {
            String accountId = entry.getKey();
            for (Map.Entry<String, Integer> rankEntry : entry.getValue().entrySet()) {
                String peerId = rankEntry.getKey();
                String key = accountId.compareTo(peerId) < 0
                        ? accountId + "|" + peerId
                        : peerId + "|" + accountId;
                String leftId = min(accountId, peerId);
                String rightId = max(accountId, peerId);
                double similarity = findSimilarity(candidates, accountId, peerId);
                int leftRank = topRanks.getOrDefault(leftId, Map.of()).getOrDefault(rightId, 0);
                int rightRank = topRanks.getOrDefault(rightId, Map.of()).getOrDefault(leftId, 0);
                edges.put(key, new BehaviorSimilarityEdge(
                        leftId,
                        rightId,
                        similarity,
                        leftRank,
                        rightRank,
                        "threshold=" + threshold + ";topN=" + topNPerAccount));
            }
        }
        return edges.values().stream()
                .sorted(Comparator
                        .comparingDouble(BehaviorSimilarityEdge::similarity).reversed()
                        .thenComparing(BehaviorSimilarityEdge::leftAccountId)
                        .thenComparing(BehaviorSimilarityEdge::rightAccountId))
                .toList();
    }

    private Map<String, Map<String, Integer>> buildTopRanks(
            Map<String, List<NeighborCandidate>> candidates,
            int topNPerAccount) {
        Map<String, Map<String, Integer>> topRanks = new HashMap<>();
        for (Map.Entry<String, List<NeighborCandidate>> entry : candidates.entrySet()) {
            List<NeighborCandidate> sorted = entry.getValue().stream()
                    .sorted(Comparator
                            .comparingDouble(NeighborCandidate::similarity).reversed()
                            .thenComparing(NeighborCandidate::peerAccountId))
                    .limit(Math.max(1, topNPerAccount))
                    .toList();
            Map<String, Integer> ranks = new LinkedHashMap<>();
            for (int i = 0; i < sorted.size(); i++) {
                ranks.put(sorted.get(i).peerAccountId(), i + 1);
            }
            topRanks.put(entry.getKey(), ranks);
        }
        return topRanks;
    }

    private double findSimilarity(
            Map<String, List<NeighborCandidate>> candidates,
            String accountId,
            String peerId) {
        return candidates.getOrDefault(accountId, List.of()).stream()
                .filter(candidate -> candidate.peerAccountId().equals(peerId))
                .mapToDouble(NeighborCandidate::similarity)
                .findFirst()
                .orElse(0.0);
    }

    private String min(String left, String right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private String max(String left, String right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private record NeighborCandidate(String peerAccountId, double similarity) {
    }
}
