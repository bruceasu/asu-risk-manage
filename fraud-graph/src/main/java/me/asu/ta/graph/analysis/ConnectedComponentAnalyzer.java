package me.asu.ta.graph.analysis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphClusterType;
import me.asu.ta.graph.model.GraphEdge;
import org.springframework.stereotype.Component;

@Component
public class ConnectedComponentAnalyzer {
    public List<GraphClusterMembership> detectClusters(List<GraphEdge> edges, Instant graphWindowStart, Instant graphWindowEnd) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (GraphEdge edge : edges) {
            adjacency.computeIfAbsent(edge.fromAccountId(), ignored -> new HashSet<>()).add(edge.toAccountId());
            adjacency.computeIfAbsent(edge.toAccountId(), ignored -> new HashSet<>()).add(edge.fromAccountId());
        }

        List<GraphClusterMembership> memberships = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Instant createdAt = Instant.now();

        for (String accountId : adjacency.keySet().stream().sorted().toList()) {
            if (!visited.add(accountId)) {
                continue;
            }
            List<String> component = new ArrayList<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(accountId);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                component.add(current);
                for (String neighbor : adjacency.getOrDefault(current, Set.of())) {
                    if (visited.add(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }
            component.sort(Comparator.naturalOrder());
            String clusterId = clusterId(component, graphWindowStart, graphWindowEnd);
            for (String member : component) {
                memberships.add(new GraphClusterMembership(
                        clusterId,
                        member,
                        GraphClusterType.MIXED,
                        component.size(),
                        graphWindowStart,
                        graphWindowEnd,
                        createdAt));
            }
        }
        return memberships;
    }

    private String clusterId(List<String> component, Instant graphWindowStart, Instant graphWindowEnd) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(String.join(",", component).getBytes(StandardCharsets.UTF_8));
            digest.update(graphWindowStart.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(graphWindowEnd.toString().getBytes(StandardCharsets.UTF_8));
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder("cluster-");
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate cluster id", ex);
        }
    }
}
