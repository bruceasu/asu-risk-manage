package me.asu.ta.graph.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.graph.model.AccountGraphSignal;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphEdge;
import me.asu.ta.graph.model.GraphRiskSummary;
import me.asu.ta.graph.repository.AccountGraphClusterRepository;
import me.asu.ta.graph.repository.AccountGraphEdgeRepository;
import me.asu.ta.graph.repository.GraphRiskSummaryRepository;
import me.asu.ta.graph.signal.GraphSignalService;
import org.springframework.stereotype.Service;

@Service
public class GraphFacade {
    private final GraphBuildService graphBuildService;
    private final GraphSignalService graphSignalService;
    private final AccountGraphEdgeRepository accountGraphEdgeRepository;
    private final AccountGraphClusterRepository accountGraphClusterRepository;
    private final GraphRiskSummaryRepository graphRiskSummaryRepository;

    public GraphFacade(
            GraphBuildService graphBuildService,
            GraphSignalService graphSignalService,
            AccountGraphEdgeRepository accountGraphEdgeRepository,
            AccountGraphClusterRepository accountGraphClusterRepository,
            GraphRiskSummaryRepository graphRiskSummaryRepository) {
        this.graphBuildService = graphBuildService;
        this.graphSignalService = graphSignalService;
        this.accountGraphEdgeRepository = accountGraphEdgeRepository;
        this.accountGraphClusterRepository = accountGraphClusterRepository;
        this.graphRiskSummaryRepository = graphRiskSummaryRepository;
    }

    public GraphAnalysisSnapshot buildWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return graphBuildService.buildWindow(graphWindowStart, graphWindowEnd);
    }

    public Optional<AccountGraphSignal> getSignalByAccountId(String accountId) {
        return graphSignalService.findByAccountId(accountId);
    }

    public List<GraphEdge> getEdgesByAccountId(String accountId) {
        return accountGraphEdgeRepository.findByAccountId(accountId);
    }

    public List<GraphClusterMembership> getClustersByAccountId(String accountId) {
        return accountGraphClusterRepository.findByAccountId(accountId);
    }

    public Optional<GraphRiskSummary> getClusterSummary(String clusterId) {
        return graphRiskSummaryRepository.findByClusterId(clusterId);
    }
}
