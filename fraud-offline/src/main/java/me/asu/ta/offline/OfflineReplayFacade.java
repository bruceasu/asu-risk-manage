package me.asu.ta.offline;

import java.util.List;
import java.util.Map;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.offline.analysis.AnomalyAnalysisService;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.AccountSimilarityAnalysisService;
import me.asu.ta.offline.analysis.BaselineAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterAnalysisService;
import me.asu.ta.offline.analysis.BehaviorClusterMember;
import me.asu.ta.offline.analysis.BehaviorFeatureAnalysisService;
import me.asu.ta.offline.analysis.BehaviorSimilarityEdge;
import me.asu.ta.offline.analysis.BotIndicatorAnalysisService;
import me.asu.ta.offline.analysis.ClusterAnalysisService;
import me.asu.ta.offline.integration.OfflineAnalysisBundle;
import me.asu.ta.offline.integration.OfflineBatchIntegrationResult;
import me.asu.ta.offline.integration.OfflineRiskBatchService;
import me.asu.ta.offline.integration.OfflineSystemIntegrationApplication;
import me.asu.ta.offline.io.OfflineChartWriter;
import me.asu.ta.offline.io.OfflineCsvWriter;
import me.asu.ta.offline.io.OfflineReportWriter;
import org.springframework.context.ConfigurableApplicationContext;

public final class OfflineReplayFacade {
    private final BaselineAnalysisService baselineAnalysisService;
    private final BotIndicatorAnalysisService botIndicatorAnalysisService;
    private final AnomalyAnalysisService anomalyAnalysisService;
    private final ClusterAnalysisService clusterAnalysisService;
    private final BehaviorFeatureAnalysisService behaviorFeatureAnalysisService;
    private final BehaviorClusterAnalysisService behaviorClusterAnalysisService;
    private final AccountSimilarityAnalysisService accountSimilarityAnalysisService;
    private final OfflineCsvWriter csvWriter;
    private final OfflineReportWriter reportWriter;
    private final OfflineChartWriter chartWriter;

    public OfflineReplayFacade() {
        this(
                new BaselineAnalysisService(),
                new BotIndicatorAnalysisService(),
                new AnomalyAnalysisService(),
                new ClusterAnalysisService(),
                new BehaviorFeatureAnalysisService(),
                new BehaviorClusterAnalysisService(),
                new AccountSimilarityAnalysisService(),
                new OfflineCsvWriter(),
                new OfflineReportWriter(),
                new OfflineChartWriter());
    }

    OfflineReplayFacade(
            BaselineAnalysisService baselineAnalysisService,
            BotIndicatorAnalysisService botIndicatorAnalysisService,
            AnomalyAnalysisService anomalyAnalysisService,
            ClusterAnalysisService clusterAnalysisService,
            BehaviorFeatureAnalysisService behaviorFeatureAnalysisService,
            BehaviorClusterAnalysisService behaviorClusterAnalysisService,
            AccountSimilarityAnalysisService accountSimilarityAnalysisService,
            OfflineCsvWriter csvWriter,
            OfflineReportWriter reportWriter,
            OfflineChartWriter chartWriter) {
        this.baselineAnalysisService = baselineAnalysisService;
        this.botIndicatorAnalysisService = botIndicatorAnalysisService;
        this.anomalyAnalysisService = anomalyAnalysisService;
        this.clusterAnalysisService = clusterAnalysisService;
        this.behaviorFeatureAnalysisService = behaviorFeatureAnalysisService;
        this.behaviorClusterAnalysisService = behaviorClusterAnalysisService;
        this.accountSimilarityAnalysisService = accountSimilarityAnalysisService;
        this.csvWriter = csvWriter;
        this.reportWriter = reportWriter;
        this.chartWriter = chartWriter;
    }

    public void execute(ReplayCliOptions options) throws Exception {
        System.out.println("Replaying trades: " + options.getTradesPath());
        ReplayState state = ReplayEngine.replay(options.getTradesPath(), options.getQuotesPath(), options.getReplay());
        
        BaselineStats baselineStats = baselineAnalysisService.computeBaseline(state, options);
        List<Anomaly> anomalies = baselineStats == null
                ? List.of()
                : anomalyAnalysisService.computeAnomalies(state, baselineStats, options.getMinTrades());
        Map<String, Integer> clusterSizes = clusterAnalysisService.computeClusterSizes(options, state);
        OfflineBatchIntegrationResult integrationResult = integrateWithCurrentSystemIfEnabled(
                options,
                new OfflineAnalysisBundle(state, baselineStats, anomalies, clusterSizes));

        if (options.isBaseline() && baselineStats != null) {
            System.out.println("Writing baseline: " + options.getOutputs().getBaseline());
            csvWriter.writeBaseline(options.getOutputs().getBaseline(), baselineStats);
        }

        botIndicatorAnalysisService.enrichDetailRows(state);
        BehaviorOutputs behaviorOutputs = analyzeBehaviorIfEnabled(options, state);
        writeReplayOutputs(options, state);

        if (options.isCluster()) {
            System.out.println("Writing clusters: " + options.getOutputs().getCluster());
            clusterAnalysisService.writeClusters(options, state);
        }

        if (baselineStats != null && options.isReport()) {
            System.out.println("Writing report: " + options.getOutputs().getReport());
            reportWriter.writeRiskReport(options.getOutputs().getReport(), anomalies, options.getTopN(), baselineStats);
            if (integrationResult != null) {
                reportWriter.appendIntegratedRiskSummary(options.getOutputs().getReport(), integrationResult.riskResults());
            }
        }

        writeBehaviorOutputsIfEnabled(options, behaviorOutputs);

        if (options.isCharts()) {
            System.out.println("Writing charts: " + options.getOutputs().getChart());
            chartWriter.writeDashboard(options, state);
        }

        if (!state.getAccountTrackers().isEmpty()) {
            System.out.println("Writing bot indicators: " + options.getOutputs().getBotIndicators());
            csvWriter.writeBotIndicators(options.getOutputs().getBotIndicators(), state.getAccountTrackers());
        }

        System.out.println("Done.");
        System.out.println("Detail rows: " + state.getDetailRows().size());
        System.out.println("Agg account|symbol keys: " + state.getAggByAccountSymbol().size());
        System.out.println("Agg account keys: " + state.getAggByAccount().size());
    }

    private void writeReplayOutputs(ReplayCliOptions options, ReplayState state) throws Exception {
        OutputOptions outputs = options.getOutputs();
        System.out.println("Writing detail: " + outputs.getDetail());
        csvWriter.writeDetail(outputs.getDetail(), state.getDetailRows());

        System.out.println("Writing agg (account|symbol): " + outputs.getAggAccountSymbol());
        csvWriter.writeAggAccountSymbol(outputs.getAggAccountSymbol(), state.getAggByAccountSymbol());

        if (options.isAggAccount()) {
            System.out.println("Writing agg (account): " + outputs.getAggAccount());
            csvWriter.writeAggAccount(outputs.getAggAccount(), state.getAggByAccount(), options.getMinTrades(), state.getGlobalAgg());
        }
        if (options.getReplay().getBucketMin() > 0) {
            System.out.println("Writing time buckets: " + outputs.getBucket());
            csvWriter.writeBuckets(outputs.getBucket(), options.getReplay().getBucketMin(), options.getReplay().getBucketBy(), state.getBuckets());
        }
        if (options.getReplay().isQuoteAgeStats()) {
            System.out.println("Writing quote_age stats: " + outputs.getQuoteAge());
            csvWriter.writeQuoteAgeStats(outputs.getQuoteAge(), options.getReplay().getQuoteAgeScope(), state.getQuoteAgeSamples());
        }
    }

    private OfflineBatchIntegrationResult integrateWithCurrentSystemIfEnabled(
            ReplayCliOptions options,
            OfflineAnalysisBundle bundle) {
        if (!options.isIntegrateCurrentSystem()) {
            return null;
        }
        System.out.println("Integrating offline results into current feature/risk system...");
        try (ConfigurableApplicationContext context = OfflineSystemIntegrationApplication.start()) {
            OfflineRiskBatchService batchService = context.getBean(OfflineRiskBatchService.class);
            OfflineBatchIntegrationResult result = batchService.integrate(bundle);
            System.out.println("Integrated snapshots: " + result.snapshots().size());
            System.out.println("Integrated risk results: " + result.riskResults().size());
            return result;
        }
    }

    private BehaviorOutputs analyzeBehaviorIfEnabled(ReplayCliOptions options, ReplayState state) {
        if (!options.isBehaviorCluster() && !options.isSimilarityEdges()) {
            return BehaviorOutputs.empty();
        }
        System.out.println("Computing account behavior feature vectors...");
        Map<String, AccountBehaviorFeatureVector> features = behaviorFeatureAnalysisService.analyze(state, options.getMinTrades());
        List<BehaviorClusterMember> clusters = options.isBehaviorCluster()
                ? behaviorClusterAnalysisService.cluster(
                        features,
                        options.getBehaviorClusterK(),
                        options.getBehaviorClusterThreshold())
                : List.of();
        List<BehaviorSimilarityEdge> edges = options.isSimilarityEdges()
                ? accountSimilarityAnalysisService.analyze(
                        features,
                        options.getSimilarityThreshold(),
                        options.getTopSimilarPerAccount())
                : List.of();
        return new BehaviorOutputs(features, clusters, edges);
    }

    private void writeBehaviorOutputsIfEnabled(ReplayCliOptions options, BehaviorOutputs behaviorOutputs) throws Exception {
        if (behaviorOutputs.isEmpty()) {
            return;
        }
        System.out.println("Writing behavior features: " + options.getOutputs().getBehaviorFeatures());
        csvWriter.writeBehaviorFeatures(options.getOutputs().getBehaviorFeatures(), behaviorOutputs.features());
        if (options.isBehaviorCluster()) {
            System.out.println("Writing behavior clusters: " + options.getOutputs().getBehaviorClusters());
            csvWriter.writeBehaviorClusters(options.getOutputs().getBehaviorClusters(), behaviorOutputs.clusters());
        }
        if (options.isSimilarityEdges()) {
            System.out.println("Writing similarity edges: " + options.getOutputs().getSimilarityEdges());
            csvWriter.writeSimilarityEdges(options.getOutputs().getSimilarityEdges(), behaviorOutputs.edges());
        }
        System.out.println("Writing behavior report: " + options.getOutputs().getBehaviorReport());
        reportWriter.writeBehaviorReport(
                options.getOutputs().getBehaviorReport(),
                behaviorOutputs.features().size(),
                behaviorOutputs.clusters(),
                behaviorOutputs.edges());
    }

    private record BehaviorOutputs(
            Map<String, AccountBehaviorFeatureVector> features,
            List<BehaviorClusterMember> clusters,
            List<BehaviorSimilarityEdge> edges) {
        static BehaviorOutputs empty() {
            return new BehaviorOutputs(Map.of(), List.of(), List.of());
        }

        boolean isEmpty() {
            return features.isEmpty() && clusters.isEmpty() && edges.isEmpty();
        }
    }
}
