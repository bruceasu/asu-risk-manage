package me.asu.ta.graph.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphEdgeType;
import org.springframework.stereotype.Component;

@Component
public class CollectorPatternAnalyzer {
    private static final int MIN_COLLECTOR_IN_DEGREE = 3;
    private static final int MAX_COLLECTOR_OUT_DEGREE = 1;
    private static final double MIN_COLLECTOR_TRANSFER_AMOUNT = 50_000.0d;

    public Map<String, CollectorMetrics> analyze(List<GraphEdge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        Map<String, Double> incomingAmount = new HashMap<>();

        for (GraphEdge edge : edges) {
            if (edge.edgeType() != GraphEdgeType.TRANSFER) {
                continue;
            }
            outDegree.merge(edge.fromAccountId(), 1, Integer::sum);
            inDegree.merge(edge.toAccountId(), 1, Integer::sum);
            incomingAmount.merge(
                    edge.toAccountId(),
                    edge.transferAmountTotal() == null ? 0.0d : edge.transferAmountTotal(),
                    Double::sum);
            inDegree.putIfAbsent(edge.fromAccountId(), inDegree.getOrDefault(edge.fromAccountId(), 0));
            outDegree.putIfAbsent(edge.toAccountId(), outDegree.getOrDefault(edge.toAccountId(), 0));
        }

        Map<String, CollectorMetrics> result = new HashMap<>();
        for (String accountId : unionKeys(inDegree, outDegree)) {
            int inbound = inDegree.getOrDefault(accountId, 0);
            int outbound = outDegree.getOrDefault(accountId, 0);
            double inboundAmount = incomingAmount.getOrDefault(accountId, 0.0d);
            boolean collector = inbound >= MIN_COLLECTOR_IN_DEGREE
                    && outbound <= MAX_COLLECTOR_OUT_DEGREE
                    && inboundAmount >= MIN_COLLECTOR_TRANSFER_AMOUNT;
            result.put(accountId, new CollectorMetrics(collector, inbound, outbound));
        }
        return result;
    }

    private Iterable<String> unionKeys(Map<String, Integer> left, Map<String, Integer> right) {
        Map<String, Boolean> keys = new HashMap<>();
        left.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        right.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        return keys.keySet();
    }
}
