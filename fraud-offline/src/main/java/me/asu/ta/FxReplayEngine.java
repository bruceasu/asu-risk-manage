package me.asu.ta;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.dto.EventText;
import me.asu.ta.dto.QuoteEvent;

public final class FxReplayEngine {
    static final String COL_ACC = "account_id";
    static final String COL_SYM = "symbol";
    static final String COL_SIDE = "side";
    static final String COL_EXEC_T = "exec_time_ms";
    static final String COL_SIZE = "size";
    static final String COL_Q_SYM = "symbol";
    static final String COL_Q_T = "quote_time_ms";
    static final String COL_BID = "bid";
    static final String COL_ASK = "ask";

    private FxReplayEngine() {}

    public static ReplayState replay(Path tradesCsv, Path quotesPath, ReplayOptions options) throws IOException {
        System.out.println("Loading quotes: " + quotesPath);
        Map<String, QuoteSeries> quotesBySymbol = loadQuotes(quotesPath);
        ReplayState state = new ReplayState();

        try (CsvReader csv = new CsvReader(tradesCsv.toString(), ',', StandardCharsets.UTF_8)) {
            if (!csv.readHeaders()) {
                throw new IllegalArgumentException("Empty trades file");
            }

            long lines = 0;
            long used = 0;
            long skipped = 0;
            while (csv.readRecord()) {
                lines++;
                if (csv.getColumnCount() < csv.getHeaderCount()) {
                    skipped++;
                    continue;
                }

                String account = csv.get(COL_ACC).trim();
                String symbol = csv.get(COL_SYM).trim();
                Side side = me.asu.ta.util.CommonUtils.parseSide(csv.get(COL_SIDE).trim());
                Long execTime = me.asu.ta.util.CommonUtils.parseLongSafe(csv.get(COL_EXEC_T));
                if (account.isEmpty() || symbol.isEmpty() || side == null || execTime == null) {
                    skipped++;
                    continue;
                }

                double size = resolveSize(csv);
                QuoteSeries quoteSeries = quotesBySymbol.get(symbol);
                if (quoteSeries == null) {
                    skipped++;
                    continue;
                }

                Double mid0 = quoteSeries.midAtOrBefore(execTime);
                Long lastQuoteTime = quoteSeries.lastQuoteTimeAtOrBefore(execTime);
                if (mid0 == null || lastQuoteTime == null) {
                    skipped++;
                    continue;
                }

                long quoteAge = execTime - lastQuoteTime;
                DetailRow detailRow = new DetailRow(account, symbol, side.name(), execTime, size, mid0, lastQuoteTime, quoteAge);
                OfflineAccountTracker tracker = state.accountTrackers.computeIfAbsent(account, key -> new OfflineAccountTracker());

                tracker.addOrderTime(execTime);
                appendOptionalTrackerSignals(csv, tracker, mid0, side.name(), size);
                fillMarkouts(detailRow, quoteSeries, side, execTime, mid0);

                state.detailRows.add(detailRow);
                aggregate(state, account, symbol, execTime, detailRow, quoteAge, options);
                used++;
            }

            System.out.println("Trades lines processed: " + lines);
            System.out.println("Trades used: " + used);
            System.out.println("Trades skipped: " + skipped);
        }

        return state;
    }

    public static Map<String, QuoteSeries> loadQuotes(Path quotesCsv) throws IOException {
        Map<String, List<QuoteEvent>> tmp = new HashMap<>();
        try (CsvReader csv = new CsvReader(quotesCsv.toString(), ',', StandardCharsets.UTF_8)) {
            if (!csv.readHeaders()) {
                throw new IllegalArgumentException("Empty trades file");
            }

            long lines = 0;
            while (csv.readRecord()) {
                lines++;
                if (csv.getColumnCount() < csv.getHeaderCount()) {
                    continue;
                }
                String symbol = csv.get(COL_Q_SYM).trim();
                Long quoteTime = me.asu.ta.util.CommonUtils.parseLongSafe(csv.get(COL_Q_T).trim());
                Double bid = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_BID).trim());
                Double ask = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_ASK).trim());
                if (symbol.isEmpty() || quoteTime == null || bid == null || ask == null) {
                    continue;
                }
                double mid = (bid + ask) * 0.5d;
                tmp.computeIfAbsent(symbol, key -> new ArrayList<>()).add(new QuoteEvent(0, null, quoteTime, mid));
            }
            System.out.println("Quotes lines processed: " + lines);
        }

        Map<String, QuoteSeries> out = new HashMap<>();
        for (var entry : tmp.entrySet()) {
            out.put(entry.getKey(), new QuoteSeries(entry.getValue()));
        }
        System.out.println("Symbols in quotes: " + out.size());
        return out;
    }

    private static double resolveSize(CsvReader csv) {
        try {
            Double parsed = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get(COL_SIZE));
            return parsed != null ? parsed : 1.0d;
        } catch (Exception ignored) {
            return 1.0d;
        }
    }

    private static void appendOptionalTrackerSignals(
            CsvReader csv,
            OfflineAccountTracker tracker,
            double mid0,
            String side,
            double size) throws IOException {
        String eventTextStr = csv.get("eventText").trim();
        Double orderSize = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("orderSize"));
        Double takeProfit = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("takeProfit"));
        Double stopLoss = me.asu.ta.util.CommonUtils.parseDoubleSafe(csv.get("stopLoss"));

        if (!eventTextStr.isEmpty()) {
            try {
                EventText eventText = EventText.parse(eventTextStr);
                tracker.addEventText(eventText, mid0, side);
            } catch (Exception ignored) {
            }
        }

        if (orderSize != null && orderSize > 0) {
            tracker.addOrderSize(orderSize);
        } else if (size > 0) {
            tracker.addOrderSize(size);
        }

        if (takeProfit != null || stopLoss != null) {
            tracker.addTPSL(takeProfit, stopLoss, mid0, side);
        }
    }

    private static void fillMarkouts(DetailRow detailRow, QuoteSeries quoteSeries, Side side, long execTime, double mid0) {
        for (int i = 0; i < DetailRow.DELTAS_MS.length; i++) {
            long t = execTime + DetailRow.DELTAS_MS[i];
            Double mid = quoteSeries.midAtOrBefore(t);
            detailRow.mids[i] = mid;
            detailRow.marks[i] = me.asu.ta.util.CommonUtils.computeMark(side, mid0, mid);
        }
    }

    private static void aggregate(
            ReplayState state,
            String account,
            String symbol,
            long execTime,
            DetailRow detailRow,
            long quoteAge,
            ReplayOptions options) {
        String accountSymbolKey = account + "|" + symbol;
        state.aggByAccountSymbol.computeIfAbsent(accountSymbolKey, key -> new Agg())
                .add(symbol, execTime, detailRow.marks[0], detailRow.marks[1], detailRow.marks[2], detailRow.marks[3], quoteAge);
        state.aggByAccount.computeIfAbsent(account, key -> new Agg())
                .add(symbol, execTime, detailRow.marks[0], detailRow.marks[1], detailRow.marks[2], detailRow.marks[3], quoteAge);
        state.globalAgg.add(symbol, execTime, detailRow.marks[0], detailRow.marks[1], detailRow.marks[2], detailRow.marks[3], quoteAge);

        if (options.getBucketMin() > 0) {
            long bucketStart = me.asu.ta.util.CommonUtils.bucketStartMs(execTime, options.getBucketMin());
            String bucketKey = me.asu.ta.util.CommonUtils.makeBucketKey(options.getBucketBy(), bucketStart, account, symbol);
            state.buckets.computeIfAbsent(bucketKey, key -> new Agg())
                    .add(symbol, execTime, detailRow.marks[0], detailRow.marks[1], detailRow.marks[2], detailRow.marks[3], quoteAge);
        }

        if (options.isQuoteAgeStats()) {
            String quoteAgeKey = me.asu.ta.util.CommonUtils.makeQuoteAgeKey(options.getQuoteAgeScope(), account, symbol);
            state.quoteAgeSamples.computeIfAbsent(quoteAgeKey, key -> new LongSamples(options.getQuoteAgeMaxSamplesPerKey()))
                    .add(quoteAge);
        }
    }
}
