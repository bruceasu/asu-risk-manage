package me.asu.ta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.BehaviorClusterAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterMember;
import org.junit.Assert;
import org.junit.Test;

public class BehaviorClusterAnalysisServiceTest {
    @Test
    public void thresholdModeShouldGroupSimilarBehaviorVectors() {
        Map<String, AccountBehaviorFeatureVector> features = new LinkedHashMap<>();
        features.put("A1", vector("A1", new double[] {1.0, 0.0}));
        features.put("A2", vector("A2", new double[] {0.999, 0.0447}));
        features.put("A3", vector("A3", new double[] {0.0, 1.0}));

        List<BehaviorClusterMember> clusters = new BehaviorClusterAnalysisService().cluster(features, 0, 0.95);

        Assert.assertEquals(3, clusters.size());
        long largestClusterMembers = clusters.stream().filter(member -> member.clusterId() == 1).count();
        Assert.assertEquals(2L, largestClusterMembers);
    }

    @Test
    public void kMeansModeShouldReturnRequestedClusterCount() {
        Map<String, AccountBehaviorFeatureVector> features = new LinkedHashMap<>();
        features.put("A1", vector("A1", new double[] {1.0, 0.0}));
        features.put("A2", vector("A2", new double[] {0.999, 0.0447}));
        features.put("A3", vector("A3", new double[] {0.0, 1.0}));

        List<BehaviorClusterMember> clusters = new BehaviorClusterAnalysisService().cluster(features, 2, 0.90);

        Assert.assertEquals(3, clusters.size());
        Assert.assertEquals(2L, clusters.stream().map(BehaviorClusterMember::clusterId).distinct().count());
    }

    private AccountBehaviorFeatureVector vector(String accountId, double[] normalized) {
        return new AccountBehaviorFeatureVector(
                accountId,
                1L,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                normalized,
                1.0);
    }
}
