package me.asu.ta.graph.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.asu.ta.graph.analysis.ClusterRiskScorer;
import me.asu.ta.graph.analysis.CollectorPatternAnalyzer;
import me.asu.ta.graph.analysis.ConnectedComponentAnalyzer;
import me.asu.ta.graph.analysis.RiskNeighborAnalyzer;
import me.asu.ta.graph.builder.SharedBankGraphBuilder;
import me.asu.ta.graph.builder.SharedDeviceGraphBuilder;
import me.asu.ta.graph.builder.SharedIpGraphBuilder;
import me.asu.ta.graph.builder.TransferGraphBuilder;
import me.asu.ta.graph.model.AccountGraphSignal;
import me.asu.ta.graph.model.CollectorMetrics;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphRiskSummary;
import me.asu.ta.graph.repository.AccountGraphClusterRepository;
import me.asu.ta.graph.repository.AccountGraphEdgeRepository;
import me.asu.ta.graph.repository.GraphRiskSummaryRepository;
import me.asu.ta.graph.signal.GraphSignalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraphBuildService {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuildService.class);

    private final SharedDeviceGraphBuilder sharedDeviceGraphBuilder;
    private final SharedIpGraphBuilder sharedIpGraphBuilder;
    private final SharedBankGraphBuilder sharedBankGraphBuilder;
    private final TransferGraphBuilder transferGraphBuilder;
    private final ConnectedComponentAnalyzer connectedComponentAnalyzer;
    private final RiskNeighborAnalyzer riskNeighborAnalyzer;
    private final CollectorPatternAnalyzer collectorPatternAnalyzer;
    private final ClusterRiskScorer clusterRiskScorer;
    private final AccountGraphEdgeRepository accountGraphEdgeRepository;
    private final AccountGraphClusterRepository accountGraphClusterRepository;
    private final GraphRiskSummaryRepository graphRiskSummaryRepository;
    private final GraphSignalService graphSignalService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GraphBuildService(
            SharedDeviceGraphBuilder sharedDeviceGraphBuilder,
            SharedIpGraphBuilder sharedIpGraphBuilder,
            SharedBankGraphBuilder sharedBankGraphBuilder,
            TransferGraphBuilder transferGraphBuilder,
            ConnectedComponentAnalyzer connectedComponentAnalyzer,
            RiskNeighborAnalyzer riskNeighborAnalyzer,
            CollectorPatternAnalyzer collectorPatternAnalyzer,
            ClusterRiskScorer clusterRiskScorer,
            AccountGraphEdgeRepository accountGraphEdgeRepository,
            AccountGraphClusterRepository accountGraphClusterRepository,
            GraphRiskSummaryRepository graphRiskSummaryRepository,
            GraphSignalService graphSignalService,
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.sharedDeviceGraphBuilder = sharedDeviceGraphBuilder;
        this.sharedIpGraphBuilder = sharedIpGraphBuilder;
        this.sharedBankGraphBuilder = sharedBankGraphBuilder;
        this.transferGraphBuilder = transferGraphBuilder;
        this.connectedComponentAnalyzer = connectedComponentAnalyzer;
        this.riskNeighborAnalyzer = riskNeighborAnalyzer;
        this.collectorPatternAnalyzer = collectorPatternAnalyzer;
        this.clusterRiskScorer = clusterRiskScorer;
        this.accountGraphEdgeRepository = accountGraphEdgeRepository;
        this.accountGraphClusterRepository = accountGraphClusterRepository;
        this.graphRiskSummaryRepository = graphRiskSummaryRepository;
        this.graphSignalService = graphSignalService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public GraphAnalysisSnapshot buildWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        logger.info("Building graph window from {} to {}", graphWindowStart, graphWindowEnd);

        List<GraphEdge> edges = new ArrayList<>();
        edges.addAll(sharedDeviceGraphBuilder.buildEdges(graphWindowStart, graphWindowEnd));
        edges.addAll(sharedIpGraphBuilder.buildEdges(graphWindowStart, graphWindowEnd));
        edges.addAll(sharedBankGraphBuilder.buildEdges(graphWindowStart, graphWindowEnd));
        edges.addAll(transferGraphBuilder.buildEdges(graphWindowStart, graphWindowEnd));

        List<GraphClusterMembership> clusters = connectedComponentAnalyzer.detectClusters(edges, graphWindowStart, graphWindowEnd);
        Set<String> riskyAccounts = fetchHighRiskAccounts();
        Map<String, Integer> oneHopRiskNeighbors = riskNeighborAnalyzer.calculateOneHopRiskNeighbors(edges, riskyAccounts);
        Map<String, Integer> twoHopRiskNeighbors = riskNeighborAnalyzer.calculateTwoHopRiskNeighbors(edges, riskyAccounts);
        Map<String, CollectorMetrics> collectorMetrics = collectorPatternAnalyzer.analyze(edges);
        List<GraphRiskSummary> graphRiskSummaries = clusterRiskScorer.buildSummaries(
                clusters, edges, riskyAccounts, collectorMetrics, graphWindowStart, graphWindowEnd);
        Map<String, Double> clusterRiskScores = mapClusterRiskScores(clusters, graphRiskSummaries);

        GraphAnalysisSnapshot snapshot = new GraphAnalysisSnapshot(
                List.copyOf(edges),
                List.copyOf(clusters),
                Map.copyOf(oneHopRiskNeighbors),
                Map.copyOf(twoHopRiskNeighbors),
                Map.copyOf(collectorMetrics),
                Map.copyOf(clusterRiskScores),
                List.copyOf(graphRiskSummaries));

        replaceWindowData(snapshot, graphWindowStart, graphWindowEnd);
        List<AccountGraphSignal> signals = graphSignalService.buildSignals(snapshot, graphWindowStart, graphWindowEnd);
        graphSignalService.persistSignals(signals, graphWindowStart, graphWindowEnd);

        logger.info(
                "Completed graph window build: edges={}, clusters={}, signals={}",
                edges.size(),
                clusters.size(),
                signals.size());
        return snapshot;
    }

    private void replaceWindowData(GraphAnalysisSnapshot snapshot, Instant graphWindowStart, Instant graphWindowEnd) {
        graphRiskSummaryRepository.deleteByWindow(graphWindowStart, graphWindowEnd);
        accountGraphClusterRepository.deleteByWindow(graphWindowStart, graphWindowEnd);
        accountGraphEdgeRepository.deleteByWindow(graphWindowStart, graphWindowEnd);
        accountGraphEdgeRepository.batchInsert(snapshot.edges());
        accountGraphClusterRepository.batchInsert(snapshot.clusters());
        graphRiskSummaryRepository.batchInsert(snapshot.graphRiskSummaries());
    }

    private Set<String> fetchHighRiskAccounts() {
        List<String> accountIds = jdbcTemplate.query("""
                with latest_scores as (
                    select account_id, max(generated_at) as max_generated_at
                      from risk_score_result
                     group by account_id
                )
                select r.account_id
                  from risk_score_result r
                  join latest_scores l
                    on l.account_id = r.account_id
                   and l.max_generated_at = r.generated_at
                 where r.risk_level in ('HIGH', 'CRITICAL')
                """, new MapSqlParameterSource(), (rs, rowNum) -> rs.getString("account_id"));
        return new HashSet<>(accountIds);
    }

    private Map<String, Double> mapClusterRiskScores(List<GraphClusterMembership> clusters, List<GraphRiskSummary> summaries) {
        Map<String, Double> scoresByClusterId = new HashMap<>();
        for (GraphRiskSummary summary : summaries) {
            scoresByClusterId.put(summary.clusterId(), summary.clusterRiskScore() == null ? 0.0d : summary.clusterRiskScore());
        }
        Map<String, Double> scoresByAccountId = new HashMap<>();
        for (GraphClusterMembership cluster : clusters) {
            scoresByAccountId.put(cluster.accountId(), scoresByClusterId.getOrDefault(cluster.clusterId(), 0.0d));
        }
        return scoresByAccountId;
    }
}
