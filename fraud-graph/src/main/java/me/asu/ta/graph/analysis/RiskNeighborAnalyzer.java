package me.asu.ta.graph.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.model.GraphEdge;
import org.springframework.stereotype.Component;

@Component
public class RiskNeighborAnalyzer {
    public Map<String, Integer> calculateOneHopRiskNeighbors(List<GraphEdge> edges, Set<String> riskyAccounts) {
        Map<String, Set<String>> adjacency = adjacency(edges);
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            int count = 0;
            for (String neighbor : entry.getValue()) {
                if (riskyAccounts.contains(neighbor)) {
                    count++;
                }
            }
            result.put(entry.getKey(), count);
        }
        return result;
    }

    public Map<String, Integer> calculateTwoHopRiskNeighbors(List<GraphEdge> edges, Set<String> riskyAccounts) {
        Map<String, Set<String>> adjacency = adjacency(edges);
        Map<String, Integer> result = new HashMap<>();
        for (String accountId : adjacency.keySet()) {
            Set<String> oneHop = adjacency.getOrDefault(accountId, Set.of());
            Set<String> twoHopRisk = new HashSet<>();
            for (String neighbor : oneHop) {
                for (String candidate : adjacency.getOrDefault(neighbor, Set.of())) {
                    if (!candidate.equals(accountId)
                            && !oneHop.contains(candidate)
                            && riskyAccounts.contains(candidate)) {
                        twoHopRisk.add(candidate);
                    }
                }
            }
            result.put(accountId, twoHopRisk.size());
        }
        return result;
    }

    private Map<String, Set<String>> adjacency(List<GraphEdge> edges) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (GraphEdge edge : edges) {
            adjacency.computeIfAbsent(edge.fromAccountId(), ignored -> new HashSet<>()).add(edge.toAccountId());
            adjacency.computeIfAbsent(edge.toAccountId(), ignored -> new HashSet<>()).add(edge.fromAccountId());
        }
        return adjacency;
    }
}
