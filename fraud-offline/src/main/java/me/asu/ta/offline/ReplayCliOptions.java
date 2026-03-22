package me.asu.ta.offline;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import me.asu.ta.util.SimpleCli;

public final class ReplayCliOptions {
    public final Path tradesPath;
    public final Path quotesPath;
    public final ReplayOptions replay;
    public final OutputOptions outputs;
    public final boolean aggAccount;
    public final boolean cluster;
    public final int clusterK;
    public final double clusterThreshold;
    public final boolean baseline;
    public final boolean report;
    public final int topN;
    public final int minTrades;
    public final boolean charts;
    public final int chartTopN;
    public final boolean integrateCurrentSystem;
    public final boolean behaviorCluster;
    public final int behaviorClusterK;
    public final double behaviorClusterThreshold;
    public final boolean similarityEdges;
    public final double similarityThreshold;
    public final int topSimilarPerAccount;

    public ReplayCliOptions(
            Path tradesPath,
            Path quotesPath,
            ReplayOptions replay,
            OutputOptions outputs,
            boolean aggAccount,
            boolean cluster,
            int clusterK,
            double clusterThreshold,
            boolean baseline,
            boolean report,
            int topN,
            int minTrades,
            boolean charts,
            int chartTopN,
            boolean integrateCurrentSystem,
            boolean behaviorCluster,
            int behaviorClusterK,
            double behaviorClusterThreshold,
            boolean similarityEdges,
            double similarityThreshold,
            int topSimilarPerAccount) {
        this.tradesPath = tradesPath;
        this.quotesPath = quotesPath;
        this.replay = replay;
        this.outputs = outputs;
        this.aggAccount = aggAccount;
        this.cluster = cluster;
        this.clusterK = clusterK;
        this.clusterThreshold = clusterThreshold;
        this.baseline = baseline;
        this.report = report;
        this.topN = topN;
        this.minTrades = minTrades;
        this.charts = charts;
        this.chartTopN = chartTopN;
        this.integrateCurrentSystem = integrateCurrentSystem;
        this.behaviorCluster = behaviorCluster;
        this.behaviorClusterK = behaviorClusterK;
        this.behaviorClusterThreshold = behaviorClusterThreshold;
        this.similarityEdges = similarityEdges;
        this.similarityThreshold = similarityThreshold;
        this.topSimilarPerAccount = topSimilarPerAccount;
    }

    public static ReplayCliOptions fromArgs(String[] args) {
        validatePresenceFlags(args, List.of(
                "--agg-account",
                "--quoteage-stats",
                "--cluster",
                "--baseline",
                "--report",
                "--charts",
                "--integrate-current-system",
                "--behavior-cluster",
                "--similarity-edges"
        ));
        return fromCli(SimpleCli.parseArgs(args));
    }

    public static ReplayCliOptions fromCli(SimpleCli cli) {
        return new ReplayCliOptions(
                Paths.get(cli.require("--trades")),
                Paths.get(cli.require("--quotes")),
                new ReplayOptions(
                        cli.intv("--time-bucket-min", 0),
                        cli.get("--bucket-by"),
                        cli.has( "--quoteage-stats"),
                        cli.get("--quoteage-scope"),
                        cli.intv("--quoteage-max-samples", 200000)
                ),
                new OutputOptions(
                        resolveOutput(cli, "--out-detail", "markout_detail.csv"),
                        resolveOutput(cli, "--out-agg", "markout_agg_by_account_symbol.csv"),
                        resolveOutput(cli, "--out-agg-account", "markout_agg_by_account.csv"),
                        resolveOutput(cli, "--out-bucket", "markout_time_buckets.csv"),
                        resolveOutput(cli, "--out-quoteage", "quote_age_stats.csv"),
                        resolveOutput(cli, "--out-baseline", "baseline.csv"),
                        resolveOutput(cli, "--out-cluster", "clusters.csv"),
                        resolveOutput(cli, "--out-report", "risk_report.txt"),
                        resolveOutput(cli, "--out-chart", "replay_dashboard.html"),
                        resolveOutput(cli, "--out-bot-indicators", "bot_indicators.csv"),
                        resolveOutput(cli, "--out-behavior-features", "account_behavior_features.csv"),
                        resolveOutput(cli, "--out-behavior-clusters", "account_behavior_clusters.csv"),
                        resolveOutput(cli, "--out-similarity-edges", "account_similarity_edges.csv"),
                        resolveOutput(cli, "--out-behavior-report", "behavior_cluster_report.txt")
                ),
                cli.has( "--agg-account"),
                cli.has("--cluster"),
                cli.intv("--cluster-k", 0),
                cli.doublev("--cluster-threshold", 0.92),
                cli.has("--baseline"),
                cli.has("--report"),
                cli.intv("--top-n", 20),
                cli.intv("--min-trades", 0),
                cli.has("--charts"),
                cli.intv("--chart-top-n", 20),
                cli.has("--integrate-current-system"),
                cli.has("--behavior-cluster"),
                cli.intv("--behavior-cluster-k", 0),
                cli.doublev("--behavior-cluster-threshold", 0.90),
                cli.has("--similarity-edges"),
                cli.doublev("--similarity-threshold", 0.93),
                cli.intv("--top-similar-per-account", 5)
        );
    }



    public static void validatePresenceFlags(String[] args, List<String> presenceFlags) {
        for (int i = 0; i < args.length; i++) {
            String current = args[i];
            if (!presenceFlags.contains(current)) {
                continue;
            }
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException(
                        current + " does not accept a value; use " + current + " by itself");
            }
        }
    }

    public static Path resolveOutput(SimpleCli cli, String key, String defaultFileName) {
        String explicitPath = cli.get(key);
        if (explicitPath != null && !explicitPath.isBlank()) {
            return Paths.get(explicitPath);
        }
        String outDir = cli.get("--out-dir");
        if (outDir != null && !outDir.isBlank()) {
            return Paths.get(outDir).resolve(defaultFileName);
        }
        return Paths.get(defaultFileName);
    }

    public Path getTradesPath() { return tradesPath; }
    public Path getQuotesPath() { return quotesPath; }
    public ReplayOptions getReplay() { return replay; }
    public OutputOptions getOutputs() { return outputs; }
    public boolean isAggAccount() { return aggAccount; }
    public boolean isCluster() { return cluster; }
    public int getClusterK() { return clusterK; }
    public double getClusterThreshold() { return clusterThreshold; }
    public boolean isBaseline() { return baseline; }
    public boolean isReport() { return report; }
    public int getTopN() { return topN; }
    public int getMinTrades() { return minTrades; }
    public boolean isCharts() { return charts; }
    public int getChartTopN() { return chartTopN; }
    public boolean isIntegrateCurrentSystem() { return integrateCurrentSystem; }
    public boolean isBehaviorCluster() { return behaviorCluster; }
    public int getBehaviorClusterK() { return behaviorClusterK; }
    public double getBehaviorClusterThreshold() { return behaviorClusterThreshold; }
    public boolean isSimilarityEdges() { return similarityEdges; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public int getTopSimilarPerAccount() { return topSimilarPerAccount; }

}
