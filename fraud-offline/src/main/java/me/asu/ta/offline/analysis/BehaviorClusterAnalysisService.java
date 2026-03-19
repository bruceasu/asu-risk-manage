package me.asu.ta.offline.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.AccountVec;
import me.asu.ta.Cluster;
import me.asu.ta.ClusterHelper;

public final class BehaviorClusterAnalysisService {
    public List<BehaviorClusterMember> cluster(
            Map<String, AccountBehaviorFeatureVector> features,
            int k,
            double threshold) {
        if (features == null || features.isEmpty()) {
            return List.of();
        }
        List<AccountVec> vectors = features.values().stream()
                .map(feature -> new AccountVec(feature.getAccountId(), feature.getNormalizedVector()))
                .toList();
        List<Cluster> clusters = k > 0
                ? ClusterHelper.kMeans(vectors, k, 30)
                : ClusterHelper.thresholdClustering(vectors, threshold);
        Map<String, Double> norms = new LinkedHashMap<>();
        for (AccountBehaviorFeatureVector feature : features.values()) {
            norms.put(feature.getAccountId(), feature.getRawVectorNorm());
        }
        List<BehaviorClusterMember> members = new ArrayList<>();
        int clusterId = 1;
        for (Cluster cluster : clusters) {
            List<AccountVec> sortedMembers = cluster.members.stream()
                    .sorted(Comparator.comparing(member -> member.accountId))
                    .toList();
            for (AccountVec member : sortedMembers) {
                members.add(new BehaviorClusterMember(
                        clusterId,
                        member.accountId,
                        cluster.members.size(),
                        norms.getOrDefault(member.accountId, 0.0),
                        cluster.note));
            }
            clusterId++;
        }
        return members;
    }
}
