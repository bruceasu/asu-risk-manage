package me.asu.ta;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.offline.integration.OfflineAnalysisBundle;
import me.asu.ta.offline.integration.OfflineSnapshotMappingService;
import org.junit.Assert;
import org.junit.Test;

public class OfflineSnapshotMappingServiceTest {
    @Test
    public void shouldMapReplayStateToFeatureSnapshots() {
        ReplayState state = new ReplayState();
        Agg agg = new Agg();
        agg.n = 12;
        agg.symbols.add("EURUSD");
        agg.symbols.add("USDJPY");
        state.getAggByAccount().put("A1", agg);

        OfflineAccountTracker tracker = new OfflineAccountTracker();
        tracker.addOrderTime(100);
        tracker.addOrderTime(200);
        tracker.addOrderTime(300);
        tracker.addOrderTime(400);
        state.getAccountTrackers().put("A1", tracker);

        Anomaly anomaly = new Anomaly("A1", 3.6, 1.2, 0.5, 12);
        OfflineAnalysisBundle bundle = new OfflineAnalysisBundle(state, null, List.of(anomaly), Map.of("A1", 4));

        List<AccountFeatureSnapshot> snapshots = new OfflineSnapshotMappingService().mapSnapshots(bundle, Instant.parse("2026-03-16T00:00:00Z"));

        Assert.assertEquals(1, snapshots.size());
        AccountFeatureSnapshot snapshot = snapshots.getFirst();
        Assert.assertEquals("A1", snapshot.accountId());
        Assert.assertEquals(Integer.valueOf(12), snapshot.transactionCount24h());
        Assert.assertEquals(Integer.valueOf(2), snapshot.uniqueCounterpartyCount24h());
        Assert.assertEquals(Integer.valueOf(4), snapshot.graphClusterSize30d());
        Assert.assertEquals(Integer.valueOf(3), snapshot.riskNeighborCount30d());
    }
}
