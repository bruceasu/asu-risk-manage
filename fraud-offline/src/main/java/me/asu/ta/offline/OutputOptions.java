package me.asu.ta.offline;

import java.nio.file.Path;

public final class OutputOptions {
    private final Path detail;
    private final Path aggAccountSymbol;
    private final Path aggAccount;
    private final Path bucket;
    private final Path quoteAge;
    private final Path baseline;
    private final Path cluster;
    private final Path report;
    private final Path chart;
    private final Path botIndicators;
    private final Path behaviorFeatures;
    private final Path behaviorClusters;
    private final Path similarityEdges;
    private final Path behaviorReport;

    OutputOptions(
            Path detail,
            Path aggAccountSymbol,
            Path aggAccount,
            Path bucket,
            Path quoteAge,
            Path baseline,
            Path cluster,
            Path report,
            Path chart,
            Path botIndicators,
            Path behaviorFeatures,
            Path behaviorClusters,
            Path similarityEdges,
            Path behaviorReport) {
        this.detail = detail;
        this.aggAccountSymbol = aggAccountSymbol;
        this.aggAccount = aggAccount;
        this.bucket = bucket;
        this.quoteAge = quoteAge;
        this.baseline = baseline;
        this.cluster = cluster;
        this.report = report;
        this.chart = chart;
        this.botIndicators = botIndicators;
        this.behaviorFeatures = behaviorFeatures;
        this.behaviorClusters = behaviorClusters;
        this.similarityEdges = similarityEdges;
        this.behaviorReport = behaviorReport;
    }

    public Path getDetail() {
        return detail;
    }

    public Path getAggAccountSymbol() {
        return aggAccountSymbol;
    }

    public Path getAggAccount() {
        return aggAccount;
    }

    public Path getBucket() {
        return bucket;
    }

    public Path getQuoteAge() {
        return quoteAge;
    }

    public Path getBaseline() {
        return baseline;
    }

    public Path getCluster() {
        return cluster;
    }

    public Path getReport() {
        return report;
    }

    public Path getChart() {
        return chart;
    }

    public Path getBotIndicators() {
        return botIndicators;
    }

    public Path getBehaviorFeatures() {
        return behaviorFeatures;
    }

    public Path getBehaviorClusters() {
        return behaviorClusters;
    }

    public Path getSimilarityEdges() {
        return similarityEdges;
    }

    public Path getBehaviorReport() {
        return behaviorReport;
    }
}
