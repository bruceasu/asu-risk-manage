package me.asu.ta;

import me.asu.ta.offline.analysis.BotIndicatorAnalysisService;
import org.junit.Assert;
import org.junit.Test;

public class BotIndicatorAnalysisServiceTest {
    @Test
    public void enrichDetailRowsShouldPopulateBotIndicators() {
        ReplayState state = new ReplayState();
        OfflineAccountTracker tracker = new OfflineAccountTracker();
        tracker.addOrderTime(100);
        tracker.addOrderTime(200);
        tracker.addOrderTime(300);
        tracker.addOrderTime(400);
        tracker.addOrderTime(500);
        tracker.addOrderSize(1.0);
        tracker.addOrderSize(1.0);
        tracker.addOrderSize(1.0);

        DetailRow row = new DetailRow("A1", "EURUSD", "BUY", 500, 1.0, 1.1000, 500, 0);
        state.getAccountTrackers().put("A1", tracker);
        state.getDetailRows().add(row);

        new BotIndicatorAnalysisService().enrichDetailRows(state);

        Assert.assertNotNull(row.cv);
        Assert.assertNotNull(row.botScore);
        Assert.assertTrue(row.cv < 0.15);
        Assert.assertTrue(row.botScore >= 70);
        Assert.assertNotNull(row.entropy);
    }
}
