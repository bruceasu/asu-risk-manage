package me.asu.ta.graph.model;

public record CollectorMetrics(
        boolean collectorAccountFlag,
        int funnelInDegree,
        int funnelOutDegree
) {
}
