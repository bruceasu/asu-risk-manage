package me.asu.ta.graph.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class CollectorPatternAnalyzerTest {
    @Test
    void shouldDetectCollectorPatternFromTransferEdges() {
        List<GraphEdge> edges = List.of(
                transfer("acct-a", "acct-collector", 20_000.0d),
                transfer("acct-b", "acct-collector", 20_000.0d),
                transfer("acct-c", "acct-collector", 15_000.0d),
                transfer("acct-collector", "acct-out", 1_000.0d));

        CollectorPatternAnalyzer analyzer = new CollectorPatternAnalyzer();
        Map<String, CollectorMetrics> metrics = analyzer.analyze(edges);

        CollectorMetrics collector = metrics.get("acct-collector");
        assertTrue(collector.collectorAccountFlag());
        assertEquals(3, collector.funnelInDegree());
        assertEquals(1, collector.funnelOutDegree());
    }

    private GraphEdge transfer(String from, String to, double amount) {
        return new GraphEdge(
                0L,
                from,
                to,
                GraphEdgeType.TRANSFER,
                1.0d,
                null,
                1,
                amount,
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END.minusSeconds(1),
                GraphTestSupport.WINDOW_START,
                GraphTestSupport.WINDOW_END,
                GraphTestSupport.FIXED_TIME);
    }
}
