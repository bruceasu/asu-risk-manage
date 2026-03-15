package me.asu.ta;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FxReplayClustererTest {
    @Test
    public void thresholdClusteringShouldGroupSimilarVectors() {
        List<AccountVec> vecs = List.of(
                new AccountVec("A1", new double[] {1.0, 0.0}),
                new AccountVec("A2", new double[] {0.999, 0.0447}),
                new AccountVec("A3", new double[] {0.0, 1.0}));

        List<Cluster> clusters = FxReplayClusterer.thresholdClustering(vecs, 0.95);

        Assert.assertEquals(2, clusters.size());
        Assert.assertEquals(2, clusters.get(0).members.size());
    }

    @Test
    public void kMeansShouldReturnRequestedClusterCount() {
        List<AccountVec> vecs = List.of(
                new AccountVec("A1", new double[] {1.0, 0.0}),
                new AccountVec("A2", new double[] {0.999, 0.0447}),
                new AccountVec("A3", new double[] {0.0, 1.0}));

        List<Cluster> clusters = FxReplayClusterer.kMeans(vecs, 2, 5);

        Assert.assertEquals(2, clusters.size());
        Assert.assertEquals(3, clusters.stream().mapToInt(c -> c.members.size()).sum());
    }
}
