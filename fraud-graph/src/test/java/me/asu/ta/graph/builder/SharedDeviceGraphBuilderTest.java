package me.asu.ta.graph.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class SharedDeviceGraphBuilderTest {
    @Test
    void shouldGenerateSharedDeviceEdges() {
        DataSource dataSource = GraphTestSupport.createDataSource();
        GraphTestSupport.insertAccountDevice(dataSource, "acct-a", "device-1", GraphTestSupport.WINDOW_START.plusSeconds(10));
        GraphTestSupport.insertAccountDevice(dataSource, "acct-b", "device-1", GraphTestSupport.WINDOW_START.plusSeconds(20));
        GraphTestSupport.insertAccountDevice(dataSource, "acct-c", "device-2", GraphTestSupport.WINDOW_START.plusSeconds(30));

        SharedDeviceGraphBuilder builder = new SharedDeviceGraphBuilder(GraphTestSupport.jdbcTemplate(dataSource));
        List<GraphEdge> edges = builder.buildEdges(GraphTestSupport.WINDOW_START, GraphTestSupport.WINDOW_END);

        assertEquals(1, edges.size());
        GraphEdge edge = edges.getFirst();
        assertEquals("acct-a", edge.fromAccountId());
        assertEquals("acct-b", edge.toAccountId());
        assertEquals(GraphEdgeType.SHARED_DEVICE, edge.edgeType());
        assertEquals(1, edge.sharedCount());
        assertEquals(1.0d, edge.edgeWeight());
    }
}
