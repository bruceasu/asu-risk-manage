package me.asu.ta.risk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.GraphRiskSignal;
import org.junit.jupiter.api.Test;

class GraphRiskSignalResolverTest {
    private final GraphRiskSignalResolver resolver = new GraphRiskSignalResolver();

    @Test
    void shouldDeriveGraphSignalFromSnapshotUsingSharedThresholds() {
        AccountFeatureSnapshot snapshot = RiskTestSupport.snapshotBuilder("acct-graph-1")
                .riskNeighborCount30d(4)
                .graphClusterSize30d(6)
                .sharedDeviceAccounts7d(5)
                .sharedBankAccounts30d(3)
                .build();

        GraphRiskSignal signal = resolver.resolve(snapshot);

        assertEquals(100.0d, signal.graphScore());
        assertEquals(6, signal.graphClusterSize());
        assertEquals(4, signal.riskNeighborCount());
        assertEquals(5, signal.sharedDeviceAccounts());
        assertEquals(3, signal.sharedBankAccounts());
    }
}
