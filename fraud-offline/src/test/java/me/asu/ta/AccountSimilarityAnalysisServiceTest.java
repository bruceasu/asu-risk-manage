package me.asu.ta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.AccountSimilarityAnalysisService;
import me.asu.ta.offline.analysis.BehaviorSimilarityEdge;
import org.junit.Assert;
import org.junit.Test;

public class AccountSimilarityAnalysisServiceTest {
    @Test
    public void shouldKeepOnlyTopSimilarPeersPerAccount() {
        Map<String, AccountBehaviorFeatureVector> features = new LinkedHashMap<>();
        features.put("A1", vector("A1", new double[] {1.0, 0.0}));
        features.put("A2", vector("A2", new double[] {0.999, 0.0447}));
        features.put("A3", vector("A3", new double[] {0.0, 1.0}));

        List<BehaviorSimilarityEdge> edges =
                new AccountSimilarityAnalysisService().analyze(features, 0.95, 1);

        Assert.assertEquals(1, edges.size());
        BehaviorSimilarityEdge edge = edges.get(0);
        Assert.assertEquals("A1", edge.leftAccountId());
        Assert.assertEquals("A2", edge.rightAccountId());
        Assert.assertTrue(edge.similarity() > 0.95);
        Assert.assertEquals(1, edge.leftRank());
        Assert.assertEquals(1, edge.rightRank());
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
