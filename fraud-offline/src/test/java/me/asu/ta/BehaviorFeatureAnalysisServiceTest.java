package me.asu.ta;

import java.util.Map;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.BehaviorFeatureAnalysisService;
import org.junit.Assert;
import org.junit.Test;

public class BehaviorFeatureAnalysisServiceTest {
    @Test
    public void shouldBuildBehaviorVectorsFromReplayState() {
        ReplayState state = new ReplayState();
        Agg agg = new Agg();
        agg.add("EURUSD", 1_000L, 0.0010, 0.0020, 0.0030, 0.0040, 120L);
        agg.add("USDJPY", 2_000L, 0.0015, 0.0025, 0.0035, 0.0045, 240L);
        state.getAggByAccount().put("A1", agg);

        DetailRow first = new DetailRow("A1", "EURUSD", "BUY", 1_000L, 1.0, 1.1000, 900L, 120L);
        first.marks[1] = 0.0020;
        first.marks[2] = 0.0030;
        DetailRow second = new DetailRow("A1", "USDJPY", "SELL", 2_000L, 2.0, 150.0000, 1_760L, 240L);
        second.marks[1] = 0.0025;
        second.marks[2] = 0.0035;
        state.getDetailRows().add(first);
        state.getDetailRows().add(second);

        OfflineAccountTracker tracker = new OfflineAccountTracker();
        tracker.addOrderTime(1_000L);
        tracker.addOrderTime(2_000L);
        tracker.addOrderSize(1.0);
        tracker.addOrderSize(2.0);
        state.getAccountTrackers().put("A1", tracker);

        Map<String, AccountBehaviorFeatureVector> features =
                new BehaviorFeatureAnalysisService().analyze(state, 1);

        AccountBehaviorFeatureVector feature = features.get("A1");
        Assert.assertNotNull(feature);
        Assert.assertEquals(2L, feature.getTradeCount());
        Assert.assertEquals(2, feature.getSymbolCount());
        Assert.assertEquals(180.0, feature.getQuoteAgeMean(), 0.0001);
        Assert.assertEquals(180.0, feature.getQuoteAgeP50(), 0.0001);
        Assert.assertEquals(228.0, feature.getQuoteAgeP90(), 0.0001);
        Assert.assertEquals(1.5, feature.getAvgSize(), 0.0001);
        Assert.assertEquals(0.0, feature.getBuySellImbalance(), 0.0001);
        Assert.assertTrue(feature.getRawVectorNorm() > 0.0);
        Assert.assertEquals(16, feature.getNormalizedVector().length);
    }

    @Test
    public void shouldHandleMissingTrackerSignals() {
        ReplayState state = new ReplayState();
        Agg agg = new Agg();
        agg.add("EURUSD", 1_000L, 0.0010, 0.0020, 0.0030, 0.0040, 100L);
        state.getAggByAccount().put("A1", agg);

        DetailRow row = new DetailRow("A1", "EURUSD", "BUY", 1_000L, 1.0, 1.1000, 900L, 100L);
        row.marks[1] = 0.0020;
        row.marks[2] = 0.0030;
        state.getDetailRows().add(row);

        AccountBehaviorFeatureVector feature = new BehaviorFeatureAnalysisService()
                .analyze(state, 1)
                .get("A1");

        Assert.assertNotNull(feature);
        Assert.assertEquals(0.0, feature.getInterArrivalCv(), 0.0001);
        Assert.assertEquals(0, feature.getClientIpCount());
        Assert.assertEquals(0, feature.getClientTypeCount());
    }
}
