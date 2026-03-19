package me.asu.ta.offline.io;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.offline.analysis.BehaviorClusterMember;
import me.asu.ta.offline.analysis.BehaviorSimilarityEdge;
import me.asu.ta.risk.model.RiskScoreResult;

public final class OfflineReportWriter {
    public void writeRiskReport(Path out, List<Anomaly> anomalies, int topN, BaselineStats baseline) throws Exception {
        FxReplayWriter.writeReport(
                out,
                anomalies,
                topN,
                baseline.mean500,
                baseline.std500,
                baseline.meanQA,
                baseline.stdQA);
    }

    public void appendIntegratedRiskSummary(Path out, Map<String, RiskScoreResult> riskResults) throws Exception {
        if (riskResults == null || riskResults.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator());
        builder.append("Unified Risk Results").append(System.lineSeparator());
        builder.append("--------------------").append(System.lineSeparator());
        riskResults.values().stream()
                .sorted((left, right) -> Double.compare(right.riskScore(), left.riskScore()))
                .forEach(result -> builder.append(result.accountId())
                        .append(" score=").append(result.riskScore())
                        .append(" level=").append(result.riskLevel())
                        .append(" reasons=").append(result.topReasonCodes())
                        .append(System.lineSeparator()));
        Files.writeString(out, builder.toString(), java.nio.file.StandardOpenOption.APPEND);
    }

    public void writeBehaviorReport(
            Path out,
            int featureCount,
            List<BehaviorClusterMember> clusters,
            List<BehaviorSimilarityEdge> edges) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("Behavior Cluster Report").append(System.lineSeparator());
        builder.append("-----------------------").append(System.lineSeparator());
        builder.append("feature_vectors=").append(featureCount).append(System.lineSeparator());
        builder.append("cluster_members=").append(clusters.size()).append(System.lineSeparator());
        builder.append("similarity_edges=").append(edges.size()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Top Clusters").append(System.lineSeparator());
        clusters.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        BehaviorClusterMember::clusterId,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()))
                .entrySet().stream()
                .sorted((left, right) -> Integer.compare(
                        right.getValue().get(0).clusterSize(),
                        left.getValue().get(0).clusterSize()))
                .limit(5)
                .forEach(entry -> builder.append("cluster=")
                        .append(entry.getKey())
                        .append(" size=").append(entry.getValue().get(0).clusterSize())
                        .append(" note=").append(entry.getValue().get(0).note())
                        .append(" accounts=")
                        .append(entry.getValue().stream().map(BehaviorClusterMember::accountId).toList())
                        .append(System.lineSeparator()));
        builder.append(System.lineSeparator());
        builder.append("Top Similarity Edges").append(System.lineSeparator());
        edges.stream()
                .limit(10)
                .forEach(edge -> builder.append(edge.leftAccountId())
                        .append(" <-> ").append(edge.rightAccountId())
                        .append(" similarity=").append(edge.similarity())
                        .append(" ranks=").append(edge.leftRank()).append("/").append(edge.rightRank())
                        .append(System.lineSeparator()));
        Files.writeString(out, builder.toString());
    }
}
