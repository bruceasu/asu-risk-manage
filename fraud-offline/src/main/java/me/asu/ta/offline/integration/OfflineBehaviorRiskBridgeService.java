package me.asu.ta.offline.integration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.OfflineBehaviorContextKeys;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.AccountSimilarityAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterMember;
import me.asu.ta.offline.analysis.BehaviorFeatureAnalysisService;
import me.asu.ta.offline.analysis.BehaviorSimilarityEdge;
import org.springframework.stereotype.Service;

@Service
public class OfflineBehaviorRiskBridgeService {
    static final double DEFAULT_CLUSTER_THRESHOLD = 0.90d;
    static final double DEFAULT_SIMILARITY_THRESHOLD = 0.93d;
    static final int DEFAULT_TOP_SIMILAR_PER_ACCOUNT = 5;

    private final BehaviorFeatureAnalysisService behaviorFeatureAnalysisService;
    private final BehaviorClusterAnalysisService behaviorClusterAnalysisService;
    private final AccountSimilarityAnalysisService accountSimilarityAnalysisService;

    public OfflineBehaviorRiskBridgeService(
            BehaviorFeatureAnalysisService behaviorFeatureAnalysisService,
            BehaviorClusterAnalysisService behaviorClusterAnalysisService,
            AccountSimilarityAnalysisService accountSimilarityAnalysisService) {
        this.behaviorFeatureAnalysisService = behaviorFeatureAnalysisService;
        this.behaviorClusterAnalysisService = behaviorClusterAnalysisService;
        this.accountSimilarityAnalysisService = accountSimilarityAnalysisService;
    }

    public Map<String, Map<String, Object>> buildContextSignals(OfflineAnalysisBundle bundle) {
        Map<String, AccountBehaviorFeatureVector> features =
                behaviorFeatureAnalysisService.analyze(bundle.replayState(), 1);
        if (features.isEmpty()) {
            return Map.of();
        }
        List<BehaviorClusterMember> clusters = behaviorClusterAnalysisService.cluster(
                features,
                0,
                DEFAULT_CLUSTER_THRESHOLD);
        List<BehaviorSimilarityEdge> edges = accountSimilarityAnalysisService.analyze(
                features,
                DEFAULT_SIMILARITY_THRESHOLD,
                DEFAULT_TOP_SIMILAR_PER_ACCOUNT);

        Map<String, Integer> clusterSizes = new HashMap<>();
        for (BehaviorClusterMember cluster : clusters) {
            clusterSizes.put(cluster.accountId(), cluster.clusterSize());
        }

        Map<String, Integer> similarCounts = new HashMap<>();
        Map<String, Double> maxSimilarities = new HashMap<>();
        for (BehaviorSimilarityEdge edge : edges) {
            increment(similarCounts, edge.leftAccountId());
            increment(similarCounts, edge.rightAccountId());
            maxSimilarities.merge(edge.leftAccountId(), edge.similarity(), Math::max);
            maxSimilarities.merge(edge.rightAccountId(), edge.similarity(), Math::max);
        }

        Map<String, Map<String, Object>> contextSignals = new LinkedHashMap<>();
        for (String accountId : features.keySet()) {
            int clusterSize = clusterSizes.getOrDefault(accountId, 1);
            int similarAccountCount = similarCounts.getOrDefault(accountId, 0);
            double maxSimilarity = maxSimilarities.getOrDefault(accountId, 0.0d);
            double coordinatedTradingScore = coordinatedTradingScore(clusterSize, similarAccountCount, maxSimilarity);
            Map<String, Object> signals = new LinkedHashMap<>();
            signals.put(OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE, clusterSize);
            signals.put(OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT, similarAccountCount);
            signals.put(OfflineBehaviorContextKeys.BEHAVIOR_MAX_SIMILARITY, round(maxSimilarity));
            signals.put(OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE, round(coordinatedTradingScore));
            contextSignals.put(accountId, Map.copyOf(signals));
        }
        return Map.copyOf(contextSignals);
    }

    private void increment(Map<String, Integer> counters, String accountId) {
        counters.merge(accountId, 1, Integer::sum);
    }

    private double coordinatedTradingScore(int clusterSize, int similarAccountCount, double maxSimilarity) {
        double score = Math.max(0, clusterSize - 1) * 10.0d;
        score += similarAccountCount * 15.0d;
        if (maxSimilarity >= DEFAULT_SIMILARITY_THRESHOLD) {
            score += (maxSimilarity - DEFAULT_SIMILARITY_THRESHOLD) * 200.0d;
        }
        return Math.min(100.0d, score);
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
