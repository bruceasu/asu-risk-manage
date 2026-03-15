package me.asu.ta.graph.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.junit.jupiter.api.Test;

class SharedBankGraphBuilderTest {
    @Test
    void shouldGenerateSharedBankEdges() {
        DataSource dataSource = GraphTestSupport.createDataSource();
        GraphTestSupport.insertBankAccount(dataSource, "acct-a", "bank-1", GraphTestSupport.WINDOW_START.plusSeconds(10));
        GraphTestSupport.insertBankAccount(dataSource, "acct-b", "bank-1", GraphTestSupport.WINDOW_START.plusSeconds(20));
        GraphTestSupport.insertSharedBankHotspot(dataSource, "bank-hot", 11);

        SharedBankGraphBuilder builder = new SharedBankGraphBuilder(GraphTestSupport.jdbcTemplate(dataSource));
        List<GraphEdge> edges = builder.buildEdges(GraphTestSupport.WINDOW_START, GraphTestSupport.WINDOW_END);

        assertEquals(1, edges.size());
        GraphEdge edge = edges.getFirst();
        assertEquals("acct-a", edge.fromAccountId());
        assertEquals("acct-b", edge.toAccountId());
        assertEquals(GraphEdgeType.SHARED_BANK_ACCOUNT, edge.edgeType());
        assertEquals(1, edge.sharedCount());
        assertEquals(2.0d, edge.edgeWeight());
    }
}
