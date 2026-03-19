package me.asu.ta.offline.io;

import com.csvreader.CsvWriter;

import me.asu.ta.Agg;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.DetailRow;
import me.asu.ta.IntervalStats;
import me.asu.ta.LongSamples;
import me.asu.ta.offline.OfflineAccountTracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class FxReplayWriter {
    /** 工具类，不允许实例化。 */
    private FxReplayWriter() {}

    /** 写出文本版风险报告（全局基线 + TopN 异常账户）。 */
    public static void writeReport(Path out, List<Anomaly> anomalies, int topN,
            double gMean500, double gStd500, double gMeanQA, double gStdQA) throws Exception {
        try (BufferedWriter bw = Files.newBufferedWriter(out)) {
            bw.write("FX Execution Risk Analysis Report\n");
            bw.write("==================================\n\n");
            bw.write("Global Baseline:\n");
            bw.write("- Avg markout 500ms: " + gMean500 + "\n");
            bw.write("- Std markout 500ms: " + gStd500 + "\n");
            bw.write("- Avg quote_age: " + gMeanQA + " ms\n");
            bw.write("- Std quote_age: " + gStdQA + " ms\n\n");
            bw.write("Top Abnormal Accounts:\n");
            bw.write("--------------------------------\n");
            for (int i = 0; i < Math.min(topN, anomalies.size()); i++) {
                Anomaly a = anomalies.get(i);
                bw.write((i + 1) + ". Account " + a.account + "\n");
                bw.write("   Trades: " + a.trades + "\n");
                bw.write("   Z(500ms): " + me.asu.ta.util.CommonUtils.fmt(a.z500) + "\n");
                bw.write("   Z(1s): " + me.asu.ta.util.CommonUtils.fmt(a.z1s) + "\n");
                bw.write("   Z(QuoteAge): " + me.asu.ta.util.CommonUtils.fmt(a.zQA) + "\n");
                bw.write("   Risk: " + riskLevel(a.z500) + "\n\n");
            }
        }
    }

    /** 将 z-score 映射为离散风险级别描述。 */
    public static String riskLevel(double z) {
        if (z > 5) return "HIGH";
        if (z > 3) return "MEDIUM";
        if (z > 2) return "LOW";
        return "NORMAL";
    }

    /** 写逐笔明细 CSV。 */
    public static void writeDetail(Path out, List<DetailRow> rows) throws IOException {
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"account_id", "symbol", "side", "exec_time_ms",
                    "size", "mid_t0", "last_quote_time_t0", "quote_age_ms", "mid_100ms", "markout_100ms",
                    "mid_500ms", "markout_500ms", "mid_1s", "markout_1s", "mid_5s", "markout_5s",
                    "cv", "botScore", "entropy", "tpslRatio", "clientIPs", "clientTypes"});
            for (DetailRow dr : rows) {
                writer.writeRecord(new String[]{dr.account, dr.symbol, dr.side,
                        Long.toString(dr.execTimeMs), me.asu.ta.util.CommonUtils.fmt6(dr.size), me.asu.ta.util.CommonUtils.fmt8(dr.mid0),
                        Long.toString(dr.lastQuoteT0), Long.toString(dr.quoteAgeMs),
                        me.asu.ta.util.CommonUtils.fmt8(dr.mids[0]), me.asu.ta.util.CommonUtils.fmt8(dr.marks[0]),
                        me.asu.ta.util.CommonUtils.fmt8(dr.mids[1]), me.asu.ta.util.CommonUtils.fmt8(dr.marks[1]),
                        me.asu.ta.util.CommonUtils.fmt8(dr.mids[2]), me.asu.ta.util.CommonUtils.fmt8(dr.marks[2]),
                        me.asu.ta.util.CommonUtils.fmt8(dr.mids[3]), me.asu.ta.util.CommonUtils.fmt8(dr.marks[3]),
                        dr.cv != null ? me.asu.ta.util.CommonUtils.fmt4(dr.cv) : "",
                        dr.botScore != null ? Integer.toString(dr.botScore) : "",
                        dr.entropy != null ? me.asu.ta.util.CommonUtils.fmt4(dr.entropy) : "",
                        dr.tpslRatio != null ? me.asu.ta.util.CommonUtils.fmt4(dr.tpslRatio) : "",
                        dr.clientIPCount != null ? Integer.toString(dr.clientIPCount) : "",
                        dr.clientTypes != null ? dr.clientTypes : ""});
            }
        }
    }

    /** 写 account|symbol 聚合 CSV（按 500ms 平均 markout 降序）。 */
    public static void writeAggAccountSymbol(Path out, Map<String, Agg> agg) throws IOException {
        List<Map.Entry<String, Agg>> list = new ArrayList<>(agg.entrySet());
        list.sort((a, b) -> Double.compare(
                me.asu.ta.util.CommonUtils.avg(b.getValue().sumMark500, b.getValue().n),
                me.asu.ta.util.CommonUtils.avg(a.getValue().sumMark500, a.getValue().n)));
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"account_id", "symbol", "n", "avg_markout_100ms",
                    "pos_ratio_100ms", "avg_markout_500ms", "pos_ratio_500ms", "avg_markout_1s",
                    "pos_ratio_1s", "avg_markout_5s", "pos_ratio_5s", "avg_quote_age_ms"});
            for (var e : list) {
                String[] parts = e.getKey().split("\\|", 2);
                String acc = parts[0];
                String sym = parts.length > 1 ? parts[1] : "";
                Agg a = e.getValue();
                double n = a.n;
                writer.writeRecord(new String[]{acc, sym, Long.toString(a.n),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark100, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos100, n)),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark500, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos500, n)),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark1s, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos1s, n)),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark5s, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos5s, n)),
                        me.asu.ta.util.CommonUtils.fmt2(me.asu.ta.util.CommonUtils.avg(a.sumQuoteAge, n))});
            }
        }
    }

    /**
     * 写 account 聚合 CSV。
     * 附加输出统一风险分数（0-100）与风险等级，并按风险分数降序。
     */
    public static BaselineStats writeAggAccount(Path out, Map<String, Agg> aggAccount, int minTrades, Agg global) throws IOException {
        List<Map.Entry<String, Agg>> list = new ArrayList<>(aggAccount.entrySet());

        // 计算基线统计并返回，避免调用方重复计算
        BaselineStats baseline = new BaselineStats(global);

        list.sort((a, b) -> Double.compare(
                riskScore(b.getValue(), baseline.mean500, baseline.std500, baseline.mean1s, baseline.std1s, baseline.meanQA, baseline.stdQA),
                riskScore(a.getValue(), baseline.mean500, baseline.std500, baseline.mean1s, baseline.std1s, baseline.meanQA, baseline.stdQA)));

        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"account_id", "n", "avg_markout_100ms",
                    "pos_ratio_100ms", "avg_markout_500ms", "pos_ratio_500ms", "avg_markout_1s",
                    "pos_ratio_1s", "avg_markout_5s", "pos_ratio_5s", "avg_quote_age_ms",
                    "trades_per_min", "symbol_count", "start_ms", "end_ms",
                    "z_markout_500ms", "z_markout_1s", "z_quote_age", "risk_score_0_100", "risk_level"});
            for (var e : list) {
                String acc = e.getKey();
                Agg a = e.getValue();
                if (a.n < minTrades) continue;
                double n = a.n;
                double tpm = me.asu.ta.util.CommonUtils.tradesPerMin(a);
                double mean500 = me.asu.ta.util.CommonUtils.avg(a.sumMark500, n);
                double mean1s = me.asu.ta.util.CommonUtils.avg(a.sumMark1s, n);
                double meanQA = me.asu.ta.util.CommonUtils.avg(a.sumQuoteAge, n);
                double z500 = me.asu.ta.util.CommonUtils.zscore(mean500, baseline.mean500, baseline.std500);
                double z1s = me.asu.ta.util.CommonUtils.zscore(mean1s, baseline.mean1s, baseline.std1s);
                double zQA = me.asu.ta.util.CommonUtils.zscore(meanQA, baseline.meanQA, baseline.stdQA);
                double score = riskScore(a, baseline.mean500, baseline.std500, baseline.mean1s, baseline.std1s, baseline.meanQA, baseline.stdQA);
                writer.writeRecord(new String[]{acc, Long.toString(a.n),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark100, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos100, n)),
                        me.asu.ta.util.CommonUtils.fmt10(mean500),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos500, n)),
                        me.asu.ta.util.CommonUtils.fmt10(mean1s),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos1s, n)),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark5s, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos5s, n)),
                        me.asu.ta.util.CommonUtils.fmt2(meanQA),
                        me.asu.ta.util.CommonUtils.fmt4(tpm),
                        Integer.toString(a.symbols.size()),
                        Long.toString(a.minT == Long.MAX_VALUE ? 0 : a.minT),
                        Long.toString(a.maxT == Long.MIN_VALUE ? 0 : a.maxT),
                        me.asu.ta.util.CommonUtils.fmt4(z500),
                        me.asu.ta.util.CommonUtils.fmt4(z1s),
                        me.asu.ta.util.CommonUtils.fmt4(zQA),
                        me.asu.ta.util.CommonUtils.fmt2(score),
                        riskLevelByScore(score)});
            }
        }
        return baseline;
    }

    private static double riskScore(Agg a, double gMean500, double gStd500,
            double gMean1s, double gStd1s, double gMeanQA, double gStdQA) {
        double n = a.n;
        if (n <= 0) return 0;
        double mean500 = me.asu.ta.util.CommonUtils.avg(a.sumMark500, n);
        double mean1s = me.asu.ta.util.CommonUtils.avg(a.sumMark1s, n);
        double meanQA = me.asu.ta.util.CommonUtils.avg(a.sumQuoteAge, n);
        double z500 = Math.max(0, me.asu.ta.util.CommonUtils.zscore(mean500, gMean500, gStd500));
        double z1s = Math.max(0, me.asu.ta.util.CommonUtils.zscore(mean1s, gMean1s, gStd1s));
        double zQA = Math.max(0, me.asu.ta.util.CommonUtils.zscore(meanQA, gMeanQA, gStdQA));

        // 0-100 统一评分：z 指标为主，正收益比例为辅。
        double zPart = 100.0 * (0.5 * Math.min(z500, 6) + 0.3 * Math.min(z1s, 6) + 0.2 * Math.min(zQA, 6)) / 6.0;
        double pos500 = me.asu.ta.util.CommonUtils.ratio(a.pos500, n);
        double pos1s = me.asu.ta.util.CommonUtils.ratio(a.pos1s, n);
        double posPart = 100.0 * Math.max(0, ((pos500 + pos1s) * 0.5) - 0.5) / 0.5;

        // 小样本降权：样本达到 200 笔后视为满权重。
        double confidence = Math.min(1.0, n / 200.0);
        double score = confidence * (0.8 * zPart + 0.2 * posPart);
        return Math.max(0, Math.min(100, score));
    }

    private static String riskLevelByScore(double score) {
        if (score >= 80) return "HIGH";
        if (score >= 60) return "MEDIUM";
        if (score >= 40) return "LOW";
        return "NORMAL";
    }

    /** 写时间桶聚合 CSV（按桶起始时间升序）。 */
    public static void writeBuckets(Path out, int bucketMin, String bucketBy, Map<String, Agg> buckets) throws IOException {
        List<Map.Entry<String, Agg>> list = new ArrayList<>(buckets.entrySet());
        list.sort(Comparator.comparingLong(e -> me.asu.ta.util.CommonUtils.parseBucketStartFromKey(e.getKey())));
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"bucket_start_ms", "bucket_start_iso", "group_key", "n",
                    "avg_markout_500ms", "pos_ratio_500ms", "avg_markout_1s", "pos_ratio_1s",
                    "avg_quote_age_ms"});
            for (var e : list) {
                String key = e.getKey();
                long bucketStart = me.asu.ta.util.CommonUtils.parseBucketStartFromKey(key);
                String groupKey = key.substring(key.indexOf('|') + 1);
                Agg a = e.getValue();
                double n = a.n;
                writer.writeRecord(new String[]{
                        Long.toString(bucketStart),
                        Instant.ofEpochMilli(bucketStart).toString(),
                        groupKey,
                        Long.toString(a.n),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark500, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos500, n)),
                        me.asu.ta.util.CommonUtils.fmt10(me.asu.ta.util.CommonUtils.avg(a.sumMark1s, n)),
                        me.asu.ta.util.CommonUtils.fmt4(me.asu.ta.util.CommonUtils.ratio(a.pos1s, n)),
                        me.asu.ta.util.CommonUtils.fmt2(me.asu.ta.util.CommonUtils.avg(a.sumQuoteAge, n))});
            }
        }
    }

    /** 写 quote_age 分位数统计 CSV（p50/p90/p99/mean）。 */
    public static void writeQuoteAgeStats(Path out, String scope, Map<String, LongSamples> samples) throws IOException {
        List<String> keys = new ArrayList<>(samples.keySet());
        Collections.sort(keys);
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"scope_key", "count_samples", "p50_ms", "p90_ms",
                    "p99_ms", "mean_ms"});
            for (String k : keys) {
                LongSamples s = samples.get(k);
                long[] sorted = s.snapshotSorted();
                if (sorted.length == 0) continue;
                long p50 = me.asu.ta.util.CommonUtils.percentile(sorted, 0.50);
                long p90 = me.asu.ta.util.CommonUtils.percentile(sorted, 0.90);
                long p99 = me.asu.ta.util.CommonUtils.percentile(sorted, 0.99);
                double mean = Arrays.stream(sorted).average().orElse(0.0);
                writer.writeRecord(new String[]{
                        k,
                        Integer.toString(sorted.length),
                        Long.toString(p50),
                        Long.toString(p90),
                        Long.toString(p99),
                        me.asu.ta.util.CommonUtils.fmt2(mean)});
            }
        }
    }

    /** 写全局 baseline CSV（从 BaselineStats 对象）。 */
    public static void writeBaseline(Path out, BaselineStats baseline) throws IOException {
        try (CsvWriter writer =new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"scope", "avg_markout_100ms", "avg_markout_500ms",
                    "avg_markout_1s", "avg_markout_5s", "avg_quote_age_ms",
                    "std_markout_100ms", "std_markout_500ms", "std_markout_1s", "std_markout_5s", "std_quote_age_ms"});
            writer.writeRecord(new String[]{
                    "GLOBAL",
                    me.asu.ta.util.CommonUtils.fmt10(baseline.mean100),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.mean500),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.mean1s),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.mean5s),
                    me.asu.ta.util.CommonUtils.fmt2(baseline.meanQA),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.std100),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.std500),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.std1s),
                    me.asu.ta.util.CommonUtils.fmt10(baseline.std5s),
                    me.asu.ta.util.CommonUtils.fmt2(baseline.stdQA)});
        }
    }
    
    /**
     * 写bot检测指标专项CSV（账户级别）。
     * 输出每个账户的综合bot检测指标，便于筛选高风险账户。
     */
    public static void writeBotIndicators(Path out, Map<String, OfflineAccountTracker> trackers) throws IOException {
        // 按botScore降序排序
        List<Map.Entry<String, OfflineAccountTracker>> list = new ArrayList<>(trackers.entrySet());
        list.sort((a, b) -> Integer.compare(
            b.getValue().computeBotScore(),
            a.getValue().computeBotScore()
        ));
        
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{
                "account_id", "cv", "isBotLike", "botScore", "entropy", 
                "tpslRatio", "clientIPs", "clientTypes", "loginNames", "totalOrders"
            });
            for (var entry : list) {
                String acc = entry.getKey();
                OfflineAccountTracker t = entry.getValue();
                IntervalStats stats = t.computeStats();
                
                writer.writeRecord(new String[]{
                    acc,
                    stats != null ? me.asu.ta.util.CommonUtils.fmt4(stats.cv()) : "",
                    stats != null && stats.isBotLike() ? "1" : "0",
                    Integer.toString(t.computeBotScore()),
                    me.asu.ta.util.CommonUtils.fmt4(t.getEntropy()),
                    me.asu.ta.util.CommonUtils.fmt4(t.getTPSLRatio()),
                    Integer.toString(t.getClientIPCount()),
                    t.getClientTypes(),
                    Integer.toString(t.getLoginNameCount()),
                    Integer.toString(t.getTotalOrders())
                });
            }
        }
    }
}
