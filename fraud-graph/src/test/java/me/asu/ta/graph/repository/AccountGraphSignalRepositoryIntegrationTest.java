package me.asu.ta.graph.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import me.asu.ta.graph.GraphTestSupport;
import me.asu.ta.graph.model.AccountGraphSignal;
import org.junit.jupiter.api.Test;

class AccountGraphSignalRepositoryIntegrationTest {
    @Test
    void shouldPersistAndReadAccountGraphSignal() {
        DataSource dataSource = GraphTestSupport.createDataSource();
        AccountGraphSignalRepository repository = GraphTestSupport.accountGraphSignalRepository(dataSource);

        AccountGraphSignal signal = GraphTestSupport.sampleSignal("acct-signal-1");
        GraphTestSupport.insertAccountGraphSignal(dataSource, signal);

        AccountGraphSignal loaded = repository.findByAccountId("acct-signal-1").orElseThrow();
        assertEquals(44.7d, loaded.graphScore(), 0.0001d);
        assertEquals(7, loaded.graphClusterSize());
        assertEquals(2, loaded.riskNeighborCount());
        assertTrue(loaded.collectorAccountFlag());
        assertEquals(72.0d, loaded.clusterRiskScore());
    }
}
