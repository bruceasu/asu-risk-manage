package me.asu.ta.graph.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class TransferGraphBuilderTest {
    @Test
    void shouldGenerateTransferEdgesAndSkipSelfLoops() {
        DataSource dataSource = GraphTestSupport.createDataSource();
        GraphTestSupport.insertTransfer(dataSource, "acct-a", "acct-b", 100.0d, GraphTestSupport.WINDOW_START.plusSeconds(10));
        GraphTestSupport.insertTransfer(dataSource, "acct-a", "acct-b", 200.0d, GraphTestSupport.WINDOW_START.plusSeconds(20));
        GraphTestSupport.insertTransfer(dataSource, "acct-b", "acct-b", 500.0d, GraphTestSupport.WINDOW_START.plusSeconds(30));

        TransferGraphBuilder builder = new TransferGraphBuilder(GraphTestSupport.jdbcTemplate(dataSource));
        List<GraphEdge> edges = builder.buildEdges(GraphTestSupport.WINDOW_START, GraphTestSupport.WINDOW_END);

        assertEquals(1, edges.size());
        GraphEdge edge = edges.getFirst();
        assertEquals("acct-a", edge.fromAccountId());
        assertEquals("acct-b", edge.toAccountId());
        assertEquals(GraphEdgeType.TRANSFER, edge.edgeType());
        assertEquals(2, edge.transferCount());
        assertEquals(300.0d, edge.transferAmountTotal());
        assertEquals(2.03d, edge.edgeWeight(), 0.0001d);
    }
}
