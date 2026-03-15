package me.asu.ta.graph.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class ConnectedComponentAnalyzerTest {
    @Test
    void shouldDetectConnectedComponents() {
        Instant createdAt = GraphTestSupport.FIXED_TIME;
        List<GraphEdge> edges = List.of(
                edge("acct-a", "acct-b", createdAt),
                edge("acct-b", "acct-c", createdAt),
                edge("acct-d", "acct-e", createdAt));

        ConnectedComponentAnalyzer analyzer = new ConnectedComponentAnalyzer();
        List<GraphClusterMembership> memberships = analyzer.detectClusters(
                edges,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END);

        assertEquals(5, memberships.size());
        Map<String, List<GraphClusterMembership>> byCluster = memberships.stream()
                .collect(Collectors.groupingBy(GraphClusterMembership::clusterId));
        assertEquals(2, byCluster.size());
        assertEquals(List.of(2, 3), byCluster.values().stream().map(List::size).sorted().toList());
    }

    private GraphEdge edge(String from, String to, Instant createdAt) {
        return new GraphEdge(
                0L,
                from,
                to,
                GraphEdgeType.SHARED_DEVICE,
                1.0d,
                1,
                null,
                null,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END.minusSeconds(1),
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END,
                createdAt);
    }
}
