package me.asu.ta;

import java.util.Map;
import me.asu.ta.offline.analysis.AccountSimilarityAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterAnalysisService;
import me.asu.ta.offline.analysis.BehaviorFeatureAnalysisService;
import me.asu.ta.offline.integration.OfflineAnalysisBundle;
import me.asu.ta.offline.integration.OfflineBehaviorRiskBridgeService;
import org.junit.Assert;
import org.junit.Test;

public class OfflineBehaviorRiskBridgeServiceTest {
    @Test
    public void shouldBuildStableBehaviorContextSignals() {
        ReplayState state = new ReplayState();

        Agg a1 = new Agg();
        a1.add("EURUSD", 1_000L, 0.0010, 0.0020, 0.0030, 0.0040, 100L);
        a1.add("EURUSD", 2_000L, 0.0011, 0.0021, 0.0031, 0.0041, 110L);
        state.getAggByAccount().put("A1", a1);
        Agg a2 = new Agg();
        a2.add("EURUSD", 1_100L, 0.0010, 0.0020, 0.0030, 0.0040, 102L);
        a2.add("EURUSD", 2_100L, 0.0011, 0.0021, 0.0031, 0.0041, 112L);
        state.getAggByAccount().put("A2", a2);

        state.getDetailRows().add(detail("A1", 1_000L, 100L, 0.0020, 0.0030));
        state.getDetailRows().add(detail("A1", 2_000L, 110L, 0.0021, 0.0031));
        state.getDetailRows().add(detail("A2", 1_100L, 102L, 0.0020, 0.0030));
        state.getDetailRows().add(detail("A2", 2_100L, 112L, 0.0021, 0.0031));

        OfflineAccountTracker t1 = new OfflineAccountTracker();
        t1.addOrderTime(1_000L);
        t1.addOrderTime(2_000L);
        state.getAccountTrackers().put("A1", t1);
        OfflineAccountTracker t2 = new OfflineAccountTracker();
        t2.addOrderTime(1_100L);
        t2.addOrderTime(2_100L);
        state.getAccountTrackers().put("A2", t2);

        OfflineBehaviorRiskBridgeService service = new OfflineBehaviorRiskBridgeService(
                new BehaviorFeatureAnalysisService(),
                new BehaviorClusterAnalysisService(),
                new AccountSimilarityAnalysisService());

        Map<String, Map<String, Object>> signals =
                service.buildContextSignals(new OfflineAnalysisBundle(state, null, java.util.List.of(), Map.of()));

        Assert.assertEquals(2, signals.size());
        Assert.assertEquals(2, signals.get("A1").get("behaviorClusterSize"));
        Assert.assertEquals(1, signals.get("A1").get("similarAccountCount"));
        Assert.assertTrue(((Double) signals.get("A1").get("behaviorMaxSimilarity")) > 0.9d);
    }

    private DetailRow detail(String accountId, long execTime, long quoteAge, double mark500, double mark1s) {
        DetailRow row = new DetailRow(accountId, "EURUSD", "BUY", execTime, 1.0, 1.1000, execTime - quoteAge, quoteAge);
        row.marks[1] = mark500;
        row.marks[2] = mark1s;
        return row;
    }
}
