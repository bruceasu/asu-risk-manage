package me.asu.ta.graph.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Row model mapped to {@code account_graph_edge}.
 *
 * @param edgeId surrogate primary key
 * @param fromAccountId source account id, or normalized smaller account id for undirected edges
 * @param toAccountId target account id, or normalized larger account id for undirected edges
 * @param edgeType relationship type such as shared device or transfer
 * @param edgeWeight normalized edge strength used by downstream graph analysis
 * @param sharedCount number of shared entities backing the edge when applicable
 * @param transferCount transfer event count backing the edge when applicable
 * @param transferAmountTotal summed transfer amount backing the edge when applicable
 * @param firstSeenAt first event time observed for this edge inside the graph window
 * @param lastSeenAt last event time observed for this edge inside the graph window
 * @param graphWindowStart inclusive graph build window start
 * @param graphWindowEnd exclusive graph build window end
 * @param createdAt persistence creation time for this edge row
 */
public record GraphEdge(
        long edgeId,
        String fromAccountId,
        String toAccountId,
        GraphEdgeType edgeType,
        double edgeWeight,
        Integer sharedCount,
        Integer transferCount,
        Double transferAmountTotal,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant graphWindowStart,
        Instant graphWindowEnd,
        Instant createdAt
) {
    public GraphEdge {
        Objects.requireNonNull(fromAccountId, "fromAccountId");
        Objects.requireNonNull(toAccountId, "toAccountId");
        Objects.requireNonNull(edgeType, "edgeType");
        edgeWeight = Math.max(0.0d, edgeWeight);
        Objects.requireNonNull(graphWindowStart, "graphWindowStart");
        Objects.requireNonNull(graphWindowEnd, "graphWindowEnd");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
