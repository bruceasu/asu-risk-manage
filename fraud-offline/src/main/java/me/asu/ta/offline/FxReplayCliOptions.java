package me.asu.ta.offline;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;

import static me.asu.ta.util.CommonUtils.intv;
import static me.asu.ta.util.CommonUtils.parseArgs;
import static me.asu.ta.util.CommonUtils.parseDouble;
import static me.asu.ta.util.CommonUtils.parseInt;
import static me.asu.ta.util.CommonUtils.require;

@Getter
public final class FxReplayCliOptions {
    private final Path tradesPath;
    private final Path quotesPath;
    private final ReplayOptions replay;
    private final OutputOptions outputs;
    private final boolean aggAccount;
    private final boolean cluster;
    private final int clusterK;
    private final double clusterThreshold;
    private final boolean baseline;
    private final boolean report;
    private final int topN;
    private final int minTrades;
    private final boolean charts;
    private final int chartTopN;
    private final boolean integrateCurrentSystem;
    private final boolean behaviorCluster;
    private final int behaviorClusterK;
    private final double behaviorClusterThreshold;
    private final boolean similarityEdges;
    private final double similarityThreshold;
    private final int topSimilarPerAccount;

    private FxReplayCliOptions(
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

    public static FxReplayCliOptions fromArgs(String[] args) {
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
        return fromCli(parseArgs(args));
    }

    private static FxReplayCliOptions fromCli(Map<String, String> cli) {
        return new FxReplayCliOptions(
                Paths.get(require(cli, "--trades")),
                Paths.get(require(cli, "--quotes")),
                new ReplayOptions(
                        parseInt(cli.getOrDefault("--time-bucket-min", "0")),
                        cli.get("--bucket-by"),
                        hasFlag(cli, "--quoteage-stats"),
                        cli.get("--quoteage-scope"),
                        parseInt(cli.getOrDefault("--quoteage-max-samples", "200000"))
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
                        resolveOutput(cli, "--out-chart", "fx_replay_dashboard.html"),
                        resolveOutput(cli, "--out-bot-indicators", "bot_indicators.csv"),
                        resolveOutput(cli, "--out-behavior-features", "account_behavior_features.csv"),
                        resolveOutput(cli, "--out-behavior-clusters", "account_behavior_clusters.csv"),
                        resolveOutput(cli, "--out-similarity-edges", "account_similarity_edges.csv"),
                        resolveOutput(cli, "--out-behavior-report", "behavior_cluster_report.txt")
                ),
                hasFlag(cli, "--agg-account"),
                hasFlag(cli, "--cluster"),
                parseInt(cli.getOrDefault("--cluster-k", "0")),
                parseDouble(cli.getOrDefault("--cluster-threshold", "0.92")),
                hasFlag(cli, "--baseline"),
                hasFlag(cli, "--report"),
                intv(cli, "--top-n", 20),
                intv(cli, "--min-trades", 0),
                hasFlag(cli, "--charts"),
                intv(cli, "--chart-top-n", 20),
                hasFlag(cli, "--integrate-current-system"),
                hasFlag(cli, "--behavior-cluster"),
                parseInt(cli.getOrDefault("--behavior-cluster-k", "0")),
                parseDouble(cli.getOrDefault("--behavior-cluster-threshold", "0.90")),
                hasFlag(cli, "--similarity-edges"),
                parseDouble(cli.getOrDefault("--similarity-threshold", "0.93")),
                intv(cli, "--top-similar-per-account", 5)
        );
    }

    private static boolean hasFlag(Map<String, String> cli, String key) {
        return cli.containsKey(key);
    }

    private static void validatePresenceFlags(String[] args, List<String> presenceFlags) {
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

    private static Path resolveOutput(Map<String, String> cli, String key, String defaultFileName) {
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

}
