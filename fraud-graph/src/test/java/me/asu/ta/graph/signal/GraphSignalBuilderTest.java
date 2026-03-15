package me.asu.ta.graph.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.AccountGraphSignal;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphClusterType;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import me.asu.ta.graph.model.GraphRiskSummary;
import org.junit.jupiter.api.Test;

class GraphSignalBuilderTest {
    @Test
    void shouldCalculateStableAccountLevelGraphSignals() {
        GraphAnalysisSnapshot snapshot = new GraphAnalysisSnapshot(
                List.of(
                        edge("acct-a", "acct-b", GraphEdgeType.SHARED_DEVICE),
                        edge("acct-a", "acct-c", GraphEdgeType.SHARED_IP),
                        edge("acct-a", "acct-d", GraphEdgeType.SHARED_BANK_ACCOUNT),
                        edge("acct-e", "acct-a", GraphEdgeType.TRANSFER),
                        edge("acct-f", "acct-a", GraphEdgeType.TRANSFER),
                        edge("acct-g", "acct-a", GraphEdgeType.TRANSFER)),
                List.of(
                        cluster("acct-a"), cluster("acct-b"), cluster("acct-c"), cluster("acct-d"),
                        cluster("acct-e"), cluster("acct-f"), cluster("acct-g")),
                Map.of("acct-a", 2),
                Map.of("acct-a", 1),
                Map.of("acct-a", new CollectorMetrics(true, 3, 1)),
                Map.of("acct-a", 72.0d),
                List.of(new GraphRiskSummary(
                        "cluster-1",
                        GraphClusterType.MIXED,
                        7,
                        2,
                        1,
                        1,
                        1,
                        3,
                        true,
                        72.0d,
                        GraphTestSupport.WINDOW_START,
                        GraphTestSupport.WINDOW_END,
                        GraphTestSupport.FIXED_TIME)));

        GraphSignalBuilder builder = new GraphSignalBuilder();
        List<AccountGraphSignal> signals = builder.buildSignals(
                snapshot,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END);

        AccountGraphSignal signal = signals.stream()
                .filter(item -> "acct-a".equals(item.accountId()))
                .findFirst()
                .orElseThrow();

        assertEquals(7, signal.graphClusterSize());
        assertEquals(2, signal.riskNeighborCount());
        assertEquals(1, signal.twoHopRiskNeighborCount());
        assertEquals(1, signal.sharedDeviceAccounts());
        assertEquals(1, signal.sharedIpAccounts());
        assertEquals(1, signal.sharedBankAccounts());
        assertTrue(signal.collectorAccountFlag());
        assertEquals(3, signal.funnelInDegree());
        assertEquals(1, signal.funnelOutDegree());
        assertEquals(100.0d, signal.localDensityScore());
        assertEquals(72.0d, signal.clusterRiskScore());
        assertEquals(44.7d, signal.graphScore(), 0.0001d);
    }

    private GraphEdge edge(String from, String to, GraphEdgeType edgeType) {
        return new GraphEdge(
                0L,
                from,
                to,
                edgeType,
                1.0d,
                edgeType == GraphEdgeType.TRANSFER ? null : 1,
                edgeType == GraphEdgeType.TRANSFER ? 1 : null,
                edgeType == GraphEdgeType.TRANSFER ? 10_000.0d : null,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END.minusSeconds(1),
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END,
                GraphTestSupport.FIXED_TIME);
    }

    private GraphClusterMembership cluster(String accountId) {
        return new GraphClusterMembership(
                "cluster-1",
                accountId,
                GraphClusterType.MIXED,
                7,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END,
                GraphTestSupport.FIXED_TIME);
    }
}
