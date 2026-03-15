package me.asu.ta;

import java.nio.file.Path;
import java.util.*;

import static me.asu.ta.util.CommonUtils.*;

/**
 * Offline FX replay analysis entry point.
 *
 * <p>This tool reads a trades CSV and a quotes CSV, replays historical executions against quote
 * history, and writes analysis outputs such as markout detail, account aggregates, time buckets,
 * quote age statistics, baseline metrics, anomaly reports, clusters, charts, and account-level bot
 * indicators.</p>
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li>Computes markout across multiple horizons such as 100ms, 500ms, 1s, and 5s.</li>
 *   <li>Aggregates results by account, symbol, account|symbol, and optional time buckets.</li>
 *   <li>Collects quote age samples and can export percentile statistics.</li>
 *   <li>Builds baseline statistics and z-score based anomaly reports from account aggregates.</li>
 *   <li>Optionally exports clustering output, bot indicators, and an HTML dashboard.</li>
 * </ul>
 *
 * <h2>CLI Notes</h2>
 * <ul>
 *   <li>Required arguments: {@code --trades <file>} and {@code --quotes <file>}.</li>
 *   <li>Boolean features use presence flags such as {@code --baseline}, {@code --report},
 *       {@code --charts}, and {@code --cluster}.</li>
 *   <li>Output files may be configured individually, or together with {@code --out-dir <dir>}.</li>
 * </ul>
 *
 * <h2>Primary Outputs</h2>
 * <ul>
 *   <li>{@code markout_detail.csv}: per-trade detail including markout, quote age, and bot fields.</li>
 *   <li>{@code markout_agg_by_account_symbol.csv}: aggregate by account|symbol.</li>
 *   <li>{@code markout_agg_by_account.csv}: aggregate by account.</li>
 *   <li>{@code markout_time_buckets.csv}: aggregate by time bucket.</li>
 *   <li>{@code quote_age_stats.csv}: quote age percentile statistics.</li>
 *   <li>{@code baseline.csv}: global baseline metrics.</li>
 *   <li>{@code clusters.csv}: account clustering result.</li>
 *   <li>{@code bot_indicators.csv}: account-level bot indicators.</li>
 *   <li>{@code risk_report.txt}: anomaly and risk summary.</li>
 *   <li>{@code fx_replay_dashboard.html}: HTML dashboard.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * java -cp target/classes me.asu.ta.FxReplayPlus \
 *   --trades trades.csv \
 *   --quotes quotes.csv \
 *   --agg-account \
 *   --baseline \
 *   --report \
 *   --out-dir out
 * }</pre>
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Parse CLI arguments into {@link FxReplayCliOptions}.</li>
 *   <li>Run the replay via {@link FxReplayEngine#replay(Path, Path, ReplayOptions)}.</li>
 *   <li>Optionally compute baseline, anomalies, bot indicators, clusters, and charts.</li>
 *   <li>Write outputs through {@link FxReplayWriter}.</li>
 * </ol>
 *
 * @see FxReplayEngine
 * @see FxReplayWriter
 * @see ReplayOptions
 * @see OutputOptions
 * @see ReplayState
 */
public class FxReplayPlus {
    /**
     * 离线回放入口：
     * 1) 读取参数与数据；
     * 2) 执行回放；
     * 3) 按开关输出明细/聚合/分位数/聚类/报告。
     */
    public static void main(String[] args) throws Exception {
        if (hasHelpArg(args)) {
            printHelp();
            return;
        }

        FxReplayCliOptions options = FxReplayCliOptions.fromArgs(args);

        System.out.println("Replaying trades: " + options.tradesPath);
        ReplayState state = FxReplayEngine.replay(
                options.tradesPath,
                options.quotesPath,
                options.replay);

        // 计算并写入账户聚合数据，同时获取基线统计
        BaselineStats baselineStats = null;
        if (options.aggAccount) {
            System.out.println("Writing agg (account): " + options.outputs.aggAccount);
            baselineStats = FxReplayWriter.writeAggAccount(
                    options.outputs.aggAccount,
                    state.aggByAccount,
                    options.minTrades,
                    state.globalAgg);
        } else {
            // 即使不输出聚合数据，如果需要基线统计也要计算（baseline 或 report 需要）
            if (options.baseline || options.report) {
                baselineStats = new BaselineStats(state.globalAgg);
            }
        }

        // 输出基线统计（使用已计算的 baselineStats，无重复计算）
        if (options.baseline && baselineStats != null) {
            System.out.println("Writing baseline: " + options.outputs.baseline);
            FxReplayWriter.writeBaseline(options.outputs.baseline, baselineStats);
        }

        // 新增：为每个DetailRow设置bot检测指标
        System.out.println("Computing bot indicators for " + state.accountTrackers.size() + " accounts...");
        for (DetailRow dr : state.detailRows) {
            OfflineAccountTracker tracker = state.accountTrackers.get(dr.account);
            if (tracker != null) {
                IntervalStats stats = tracker.computeStats();
                dr.setBotIndicators(
                    stats,
                    tracker.getEntropy(),
                    tracker.getTPSLRatio(),
                    tracker.getClientIPCount(),
                    tracker.getClientTypes()
                );
            }
        }

        System.out.println("Writing detail: " + options.outputs.detail);
        FxReplayWriter.writeDetail(options.outputs.detail, state.detailRows);

        System.out.println("Writing agg (account|symbol): " + options.outputs.aggAccountSymbol);
        FxReplayWriter.writeAggAccountSymbol(options.outputs.aggAccountSymbol, state.aggByAccountSymbol);

        if (options.replay.bucketMin > 0) {
            System.out.println("Writing time buckets: " + options.outputs.bucket);
            FxReplayWriter.writeBuckets(options.outputs.bucket, options.replay.bucketMin, options.replay.bucketBy, state.buckets);
        }
        if (options.replay.quoteAgeStats) {
            System.out.println("Writing quote_age stats: " + options.outputs.quoteAge);
            FxReplayWriter.writeQuoteAgeStats(options.outputs.quoteAge, options.replay.quoteAgeScope, state.quoteAgeSamples);
        }
        if (options.cluster) {
            System.out.println("Writing clusters: " + options.outputs.cluster);
            FxReplayClusterer.clusterAccountsAndWrite(options.outputs.cluster, state.aggByAccount, options.clusterK, options.clusterThreshold, options.minTrades);
        }

        if (baselineStats != null) {
            System.out.println("Computing anomalies (using cached baseline)...");

            List<Anomaly> anomalies = new ArrayList<>();
            for (var e : state.aggByAccount.entrySet()) {
                Agg a = e.getValue();
                if (a.n < options.minTrades) continue;
                double mean500 = mean(a.sumMark500, a.n);
                double mean1s = mean(a.sumMark1s, a.n);
                double meanQA = mean(a.sumQuoteAge, a.n);
                double z500 = zscore(mean500, baselineStats.mean500, baselineStats.std500);
                double z1s = zscore(mean1s, baselineStats.mean1s, baselineStats.std1s);
                double zQA = zscore(meanQA, baselineStats.meanQA, baselineStats.stdQA);
                anomalies.add(new Anomaly(e.getKey(), z500, z1s, zQA, a.n));
            }
            anomalies.sort((a, b) -> Double.compare(b.z500, a.z500));
            if (options.report) {
                FxReplayWriter.writeReport(
                        options.outputs.report,
                        anomalies,
                        options.topN,
                        baselineStats.mean500,
                        baselineStats.std500,
                        baselineStats.meanQA,
                        baselineStats.stdQA);
            }
        }
        if (options.charts) {
            System.out.println("Writing charts: " + options.outputs.chart);
            me.asu.ta.util.Charts.writeDashboard(
                    options.outputs.chart, state.aggByAccount, options.minTrades, options.chartTopN, options.tradesPath, options.quotesPath);
        }

        // 新增：输出bot检测指标专项CSV
        if (!state.accountTrackers.isEmpty()) {
            System.out.println("Writing bot indicators: " + options.outputs.botIndicators);
            FxReplayWriter.writeBotIndicators(options.outputs.botIndicators, state.accountTrackers);
        }

        System.out.println("Done.");
        System.out.println("Detail rows: " + state.detailRows.size());
        System.out.println("Agg account|symbol keys: " + state.aggByAccountSymbol.size());
        System.out.println("Agg account keys: " + state.aggByAccount.size());
    }

    /** 检查是否请求帮助信息。 */
    private static boolean hasHelpArg(String[] args) {
        for (String a : args) {
            if ("-h".equals(a) || "--help".equals(a)) {
                return true;
            }
        }
        return false;
    }

    /** 输出命令行参数说明和参考示例。 */
    private static void printHelp() {
        System.out.println("""
                FxReplayPlus - Offline FX Replay Tool

                Usage:
                  java -jar bin/app.jar --trades <file> --quotes <file> [options]
                  java -cp classes FxReplayPlus --trades <file> --quotes <file> [options]

                Required:
                  --trades <file>              trades CSV
                  --quotes <file>              quotes CSV

                Output:
                  --out-dir <dir>              apply default output names under one directory
                  --out-detail <file>          default: markout_detail.csv
                  --out-agg <file>             default: markout_agg_by_account_symbol.csv
                  --out-agg-account <file>     default: markout_agg_by_account.csv
                  --out-bucket <file>          default: markout_time_buckets.csv
                  --out-quoteage <file>        default: quote_age_stats.csv
                  --out-baseline <file>        default: baseline.csv
                  --out-cluster <file>         default: clusters.csv
                  --out-report <file>          default: risk_report.txt
                  --out-chart <file>           default: fx_replay_dashboard.html
                  --out-bot-indicators <file>  default: bot_indicators.csv

                Feature switches:
                  --agg-account                default: off
                  --min-trades <N>             default: 0
                  --time-bucket-min <N>        default: 0 (off)
                  --bucket-by all|account|symbol|account_symbol   default: all
                  --quoteage-stats             default: off
                  --quoteage-scope all|account|symbol|account_symbol  default: all
                  --quoteage-max-samples <N>   default: 200000
                  --cluster                    default: off
                  --cluster-k <K>              default: 0 (threshold mode)
                  --cluster-threshold <T>      default: 0.92
                  --baseline                   default: off
                  --report                     default: off
                  --top-n <N>                  default: 20
                  --charts                     default: off
                  --chart-top-n <N>            default: 20

                Examples:
                  Basic:
                    java -jar bin/app.jar --trades offline/examples/trades.csv --quotes offline/examples/quotes.csv

                  Directory-based output:
                    java -jar bin/app.jar --trades offline/examples/trades.csv --quotes offline/examples/quotes.csv --out-dir out

                  Full analysis:
                    java -jar bin/app.jar ^
                      --trades offline/examples/trades.csv ^
                      --quotes offline/examples/quotes.csv ^
                      --agg-account ^
                      --time-bucket-min 1 ^
                      --bucket-by all ^
                      --quoteage-stats ^
                      --cluster ^
                      --cluster-threshold 0.93 ^
                      --baseline ^
                      --report ^
                      --charts ^
                      --out-chart fx_replay_dashboard.html ^
                      --top-n 5 ^
                      --min-trades 1
                """);
    }
}