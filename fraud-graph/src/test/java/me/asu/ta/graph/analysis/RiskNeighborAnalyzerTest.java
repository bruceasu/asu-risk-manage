package me.asu.ta.graph.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class RiskNeighborAnalyzerTest {
    @Test
    void shouldCalculateOneHopAndTwoHopRiskNeighbors() {
        List<GraphEdge> edges = List.of(
                edge("acct-a", "acct-b"),
                edge("acct-b", "acct-c"),
                edge("acct-c", "acct-d"));

        RiskNeighborAnalyzer analyzer = new RiskNeighborAnalyzer();
        Set<String> riskyAccounts = Set.of("acct-c", "acct-d");

        Map<String, Integer> oneHop = analyzer.calculateOneHopRiskNeighbors(edges, riskyAccounts);
        Map<String, Integer> twoHop = analyzer.calculateTwoHopRiskNeighbors(edges, riskyAccounts);

        assertEquals(0, oneHop.get("acct-a"));
        assertEquals(1, oneHop.get("acct-b"));
        assertEquals(1, oneHop.get("acct-c"));
        assertEquals(1, oneHop.get("acct-d"));

        assertEquals(1, twoHop.get("acct-a"));
        assertEquals(1, twoHop.get("acct-b"));
        assertEquals(0, twoHop.get("acct-c"));
        assertEquals(0, twoHop.get("acct-d"));
    }

    private GraphEdge edge(String from, String to) {
        return new GraphEdge(
                0L,
                from,
                to,
                GraphEdgeType.SHARED_IP,
                1.0d,
                1,
                null,
                null,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END.minusSeconds(1),
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END,
                GraphTestSupport.FIXED_TIME);
    }
}
