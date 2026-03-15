package me.asu.ta;

import java.util.List;
import java.util.Map;
import me.asu.ta.offline.integration.OfflineAnalysisBundle;
import me.asu.ta.offline.integration.OfflineGraphBridgeService;
import me.asu.ta.risk.model.GraphRiskSignal;
import org.junit.Assert;
import org.junit.Test;

public class OfflineGraphBridgeServiceTest {
    @Test
    public void shouldBuildGraphSignalsFromClusterSizes() {
        ReplayState state = new ReplayState();
        state.getAggByAccount().put("A1", new Agg());
        state.getAggByAccount().put("A2", new Agg());

        Map<String, GraphRiskSignal> signals = new OfflineGraphBridgeService()
                .buildGraphSignals(new OfflineAnalysisBundle(state, null, List.of(), Map.of("A1", 5, "A2", 2)));

        Assert.assertEquals(2, signals.size());
        Assert.assertEquals(5, signals.get("A1").graphClusterSize());
        Assert.assertEquals(4, signals.get("A1").riskNeighborCount());
        Assert.assertTrue(signals.get("A1").graphScore() >= 70.0d);
        Assert.assertEquals(2, signals.get("A2").graphClusterSize());
        Assert.assertEquals(1, signals.get("A2").riskNeighborCount());
    }
}
