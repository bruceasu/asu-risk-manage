package me.asu.ta.graph.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class SharedIpGraphBuilderTest {
    @Test
    void shouldGenerateSharedIpEdgesAndFilterHotspots() {
        DataSource dataSource = GraphTestSupport.createDataSource();
        GraphTestSupport.insertLoginLog(dataSource, "acct-a", "10.0.0.1", "HIGH", GraphTestSupport.WINDOW_START.plusSeconds(10));
        GraphTestSupport.insertLoginLog(dataSource, "acct-b", "10.0.0.1", "HIGH", GraphTestSupport.WINDOW_START.plusSeconds(20));
        GraphTestSupport.insertSharedIpHotspot(dataSource, "10.0.0.99", 16);

        SharedIpGraphBuilder builder = new SharedIpGraphBuilder(GraphTestSupport.jdbcTemplate(dataSource));
        List<GraphEdge> edges = builder.buildEdges(GraphTestSupport.WINDOW_START, GraphTestSupport.WINDOW_END);

        assertEquals(1, edges.size());
        GraphEdge edge = edges.getFirst();
        assertEquals("acct-a", edge.fromAccountId());
        assertEquals("acct-b", edge.toAccountId());
        assertEquals(GraphEdgeType.SHARED_IP, edge.edgeType());
        assertEquals(1, edge.sharedCount());
    }
}
