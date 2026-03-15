package me.asu.ta.graph.model;

import java.util.List;
import java.util.Map;

public record GraphAnalysisSnapshot(
        List<GraphEdge> edges,
        List<GraphClusterMembership> clusters,
        Map<String, Integer> oneHopRiskNeighbors,
        Map<String, Integer> twoHopRiskNeighbors,
        Map<String, CollectorMetrics> collectorMetrics,
        Map<String, Double> clusterRiskScores,
        List<GraphRiskSummary> graphRiskSummaries
) {
}
