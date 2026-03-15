package me.asu.ta;

import com.csvreader.CsvReader;
import me.asu.ta.dto.EventText;
import me.asu.ta.dto.QuoteEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>外汇交易回放引擎</h1>
 *
 * 核心功能：逐行读取成交记录，按 symbol 对齐报价数据，计算 markout 和 quote age，
 * 并累积到多维聚合结构中，供后续分析和聚类使用。
 *
 * <h2>主要流程</h2>
 *
 * 1. 读取 quotes.csv，构建按 symbol 聚合的 QuoteSeries，支持快速查询任意时刻的 mid。
 * 2. 逐行读取 trades.csv，做字段校验和清洗，提取核心字段（account_id、symbol、side、exec_time_ms）。
 * 3. 对每笔成交，根据 symbol 对齐报价数据，获取成交时刻的 mid 和 quote age。
 * 4. 计算未来多个 horizon 的 markout，并构建 DetailRow 存储明细数据。
 * 5. 累积到 account|symbol、account、global 与可选时间桶的 Agg 中，用于后续统计分析。
 * 6. 可选：收集 quote age 样本，用于分位数统计和异常检测。
 */
final class FxReplayEngine {
    /**
     * 工具类，不允许实例化。
     */
    private FxReplayEngine() {}

    static final String COL_ACC = "account_id";
    static final String COL_SYM = "symbol";
    static final String COL_SIDE = "side";
    static final String COL_EXEC_T = "exec_time_ms";
    static final String COL_SIZE = "size";
    static final String COL_Q_SYM = "symbol";
    static final String COL_Q_T = "quote_time_ms";
    static final String COL_BID = "bid";
    static final String COL_ASK = "ask";

    /**
     * 回放成交并计算明细与多维聚合。
     * 主要流程：
     * 1) 逐行读取 trades 并做字段校验；
     * 2) 用 symbol 对应报价序列对齐成交时刻 mid；
     * 3) 计算各 horizon 的 markout；
     * 4) 累加到 account|symbol、account、global 与可选统计结构。
     */
    static ReplayState replay(
            Path tradesCsv,
            Path quotesPath,
            ReplayOptions options) throws IOException {
        System.out.println("Loading quotes: " + quotesPath);
        Map<String, QuoteSeries> quotesBySymbol = FxReplayEngine.loadQuotes(quotesPath);

        ReplayState st = new ReplayState();

        try (CsvReader csv = new CsvReader(tradesCsv.toString(), ',', StandardCharsets.UTF_8)) {
            if (!csv.readHeaders()) throw new IllegalArgumentException("Empty trades file");

            long lines = 0, used = 0, skipped = 0;
            while (csv.readRecord()) {
                lines++;
                if (csv.getColumnCount() < csv.getHeaderCount()) {
                    skipped++;
                    continue;
                }
                String acc = csv.get(COL_ACC).trim();
                String sym = csv.get(COL_SYM).trim();
                Side side = me.asu.ta.util.CommonUtils.parseSide(csv.get(COL_SIDE).trim());
                Long t0 = me.asu.ta.util.CommonUtils.parseLongSafe(csv.get(COL_EXEC_T));
                if (acc.isEmpty() || sym.isEmpty() || side == null || t0 == null) {
                    skipped++;
                    continue;
                }
                double size = 1.0;
                try {
                    Double s = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_SIZE));
                    if (s != null) size = s;
                } catch (Exception e) {
                    // 解析失败，回退到默认值 1.0
                }
                QuoteSeries qs = quotesBySymbol.get(sym);
                if (qs == null) {
                    skipped++;
                    continue;
                }
                Double mid0 = qs.midAtOrBefore(t0);
                Long lastQt0 = qs.lastQuoteTimeAtOrBefore(t0);
                if (mid0 == null || lastQt0 == null) {
                    skipped++;
                    continue;
                }
                long quoteAge = t0 - lastQt0;
                DetailRow dr = new DetailRow(acc, sym, side.name(), t0, size, mid0, lastQt0, quoteAge);

                String eventTextStr = csv.get("eventText").trim();
                Double orderSize = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("orderSize"));
                Double takeProfit = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("takeProfit"));
                Double stopLoss = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("stopLoss"));

                // 获取或创建账户追踪器
                OfflineAccountTracker tracker = st.accountTrackers.computeIfAbsent(
                        acc, k -> new OfflineAccountTracker()
                );

                // 添加订单时间（必须）
                tracker.addOrderTime(t0);

                // 添加EventText相关数据（如果存在）
                if (eventTextStr != null && !eventTextStr.isEmpty()) {
                    try {
                        EventText et = EventText.parse(eventTextStr);
                        tracker.addEventText(et, mid0, side.name());
                    } catch (Exception e) {
                        // 解析失败，忽略该EventText
                    }
                }

                // 添加订单大小（如果存在）
                if (orderSize != null && orderSize > 0) {
                    tracker.addOrderSize(orderSize);
                } else if (size > 0) {
                    // 回退到使用size字段
                    tracker.addOrderSize(size);
                }

                // 添加TP/SL（如果存在）
                if (takeProfit != null || stopLoss != null) {
                    tracker.addTPSL(takeProfit, stopLoss, mid0, side.name());
                }
                // 对每个 horizon 计算未来时刻 mid 与方向化 markout。
                for (int i = 0; i < DetailRow.DELTAS_MS.length; i++) {
                    long t = t0 + DetailRow.DELTAS_MS[i];
                    Double m = qs.midAtOrBefore(t);
                    dr.mids[i] = m;
                    dr.marks[i] = me.asu.ta.util.CommonUtils.computeMark(side, mid0, m);
                }
                st.detailRows.add(dr);

                String kAS = acc + "|" + sym;
                st.aggByAccountSymbol.computeIfAbsent(kAS, k -> new Agg())
                        .add(sym, t0, dr.marks[0], dr.marks[1], dr.marks[2], dr.marks[3], quoteAge);
                st.aggByAccount.computeIfAbsent(acc, k -> new Agg())
                        .add(sym, t0, dr.marks[0], dr.marks[1], dr.marks[2], dr.marks[3], quoteAge);
                st.globalAgg.add(sym, t0, dr.marks[0], dr.marks[1], dr.marks[2], dr.marks[3], quoteAge);

                // 可选：时间桶聚合，用于定位异常时间段。
                if (options.bucketMin > 0) {
                    long bucketStart = me.asu.ta.util.CommonUtils.bucketStartMs(t0, options.bucketMin);
                    String bucketKey = me.asu.ta.util.CommonUtils.makeBucketKey(options.bucketBy, bucketStart, acc, sym);
                    st.buckets.computeIfAbsent(bucketKey, k -> new Agg())
                            .add(sym, t0, dr.marks[0], dr.marks[1], dr.marks[2], dr.marks[3], quoteAge);
                }
                // 可选：quote_age 样本收集，用于分位数统计。
                if (options.quoteAgeStats) {
                    String key = me.asu.ta.util.CommonUtils.makeQuoteAgeKey(options.quoteAgeScope, acc, sym);
                    st.quoteAgeSamples.computeIfAbsent(key, k -> new LongSamples(options.quoteAgeMaxSamplesPerKey))
                            .add(quoteAge);
                }
                used++;

            }
            System.out.println("Trades lines processed: " + lines);
            System.out.println("Trades used: " + used);
            System.out.println("Trades skipped: " + skipped);
        }
        return st;
    }

    /**
     * 读取 quotes.csv，按 symbol 聚合并构造可二分查询的 QuoteSeries。
     * 注意：QuoteSeries 构造时会按 time 升序排序。
     */
    static Map<String, QuoteSeries> loadQuotes(Path quotesCsv) throws IOException {
        Map<String, List<QuoteEvent>> tmp = new HashMap<>();
        try (CsvReader csv = new CsvReader(quotesCsv.toString(), ',', StandardCharsets.UTF_8)) {
            if (!csv.readHeaders()) throw new IllegalArgumentException("Empty trades file");
            long lines = 0, skipped = 0;
            while (csv.readRecord()) {
                lines++;
                if (csv.getColumnCount() < csv.getHeaderCount()) {
                    skipped++;
                    continue;
                }
                String sym = csv.get(COL_Q_SYM).trim();
                Long t = me.asu.ta.util.CommonUtils.parseLongSafe(csv.get(COL_Q_T).trim());
                Double bid = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_BID).trim());
                Double ask = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_ASK).trim());
                if (sym.isEmpty() || t == null || bid == null || ask == null) continue;
                double mid = (bid + ask) * 0.5;
                // symbolId 设为 0，表示通用报价数据（非特定品种事件）
                tmp.computeIfAbsent(sym, k -> new ArrayList<>()).add(new QuoteEvent(0, null, t, mid));
            }
            System.out.println("Quotes lines processed: " + lines);
        }
        Map<String, QuoteSeries> out = new HashMap<>();
        for (var e : tmp.entrySet()) out.put(e.getKey(), new QuoteSeries(e.getValue()));
        System.out.println("Symbols in quotes: " + out.size());
        return out;
    }
}
