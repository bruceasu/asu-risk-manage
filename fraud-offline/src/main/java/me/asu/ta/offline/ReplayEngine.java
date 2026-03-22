package me.asu.ta.offline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.csvreader.CsvReader;

import me.asu.ta.Agg;
import me.asu.ta.DetailRow;
import me.asu.ta.LongSamples;
import me.asu.ta.QuoteSeries;
import me.asu.ta.Side;
import me.asu.ta.dto.EventText;
import me.asu.ta.offline.io.QuoteSeriesReader;
import me.asu.ta.util.CommonUtils;

public final class ReplayEngine {
    static final String COL_ACC = "account_id";
    static final String COL_SYM = "symbol";
    static final String COL_SIDE = "side";
    static final String COL_EXEC_T = "exec_time_ms";
    static final String COL_VOLUME = "volume";


    private ReplayEngine() {}

    public static ReplayState replay(Path tradesCsv, Path quotesPath, ReplayOptions options) throws IOException {
        System.out.println("Loading quotes: " + quotesPath);
        QuoteSeriesReader quoteSeriesUtils = new QuoteSeriesReader(quotesPath);
  
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
                Side side = CommonUtils.parseSide(csv.get(COL_SIDE).trim());
                Long execTime = CommonUtils.parseLongSafe(csv.get(COL_EXEC_T));
                if (account.isEmpty() || symbol.isEmpty() || side == null || execTime == null) {
                    skipped++;
                    continue;
                }

                double volume = resolveVolume(csv);
                OfflineAccountTracker tracker = state.accountTrackers.computeIfAbsent(account, key -> new OfflineAccountTracker());
                tracker.addOrderTime(execTime);

                QuoteSeries quoteSeries = quoteSeriesUtils.get(symbol);
                Double mid0 = null;
                Long lastQuoteTime = null;
                if (quoteSeries == null) {
                    skipped++;
                } else {
                    mid0 = quoteSeries.midAtOrBefore(execTime);
                    lastQuoteTime = quoteSeries.lastQuoteTimeAtOrBefore(execTime);
                    if (mid0 == null || lastQuoteTime == null) {
                        skipped++;
                    }
                }

                long quoteAge = lastQuoteTime == null ? -1L : execTime - lastQuoteTime;
                DetailRow detailRow = new DetailRow(
                        account,
                        symbol,
                        side.name(),
                        execTime,
                        volume,
                        mid0 == null ? Double.NaN : mid0,
                        lastQuoteTime == null ? -1L : lastQuoteTime,
                        quoteAge);

                appendOptionalTrackerSignals(csv, tracker, mid0, side.name(), volume);
                if (quoteSeries != null && mid0 != null) {
                    fillMarkouts(detailRow, quoteSeries, side, execTime, mid0);
                }

                state.detailRows.add(detailRow);
                if (mid0 != null && lastQuoteTime != null) {
                    aggregate(state, account, symbol, execTime, detailRow, quoteAge, options);
                }
                used++;
            }

            System.out.println("Trades lines processed: " + lines);
            System.out.println("Trades used: " + used);
            System.out.println("Trades skipped: " + skipped);
        }

        return state;
    }

    

    private static double resolveVolume(CsvReader csv) {
        try {
            Double parsed = CommonUtils.parseDoubleSafe(csv.get(COL_VOLUME));
            return parsed != null ? parsed : 1.0d;
        } catch (Exception ignored) {
            return 1.0d;
        }
    }

    private static void appendOptionalTrackerSignals(
            CsvReader csv,
            OfflineAccountTracker tracker,
            Double mid0,
            String side,
            double volume) throws IOException {
        String eventTextStr = csv.get("eventText").trim();
        Double takeProfit = CommonUtils.parseDoubleSafe(csv.get("takeProfit"));
        Double stopLoss = CommonUtils.parseDoubleSafe(csv.get("stopLoss"));

        if (!eventTextStr.isEmpty()) {
            try {
                EventText eventText = EventText.parse(eventTextStr);
                if (mid0 != null) {
                    tracker.addEventText(eventText, mid0, side);
                }
            } catch (Exception ignored) {
            }
        }

        tracker.addOrderVolume(volume);

        if (takeProfit != null || stopLoss != null) {
            if (mid0 != null) {
                tracker.addTPSL(takeProfit, stopLoss, mid0, side);
            }
        }
    }

    private static void fillMarkouts(DetailRow detailRow, QuoteSeries quoteSeries, Side side, long execTime, double mid0) {
        for (int i = 0; i < DetailRow.DELTAS_MS.length; i++) {
            long t = execTime + DetailRow.DELTAS_MS[i];
            Double mid = quoteSeries.midAtOrBefore(t);
            detailRow.mids[i] = mid;
            detailRow.marks[i] = CommonUtils.computeMark(side, mid0, mid);
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
            long bucketStart = CommonUtils.bucketStartMs(execTime, options.getBucketMin());
            String bucketKey = CommonUtils.makeBucketKey(options.getBucketBy(), bucketStart, account, symbol);
            state.buckets.computeIfAbsent(bucketKey, key -> new Agg())
                    .add(symbol, execTime, detailRow.marks[0], detailRow.marks[1], detailRow.marks[2], detailRow.marks[3], quoteAge);
        }

        if (options.isQuoteAgeStats()) {
            String quoteAgeKey = CommonUtils.makeQuoteAgeKey(options.getQuoteAgeScope(), account, symbol);
            state.quoteAgeSamples.computeIfAbsent(quoteAgeKey, key -> new LongSamples(options.getQuoteAgeMaxSamplesPerKey()))
                    .add(quoteAge);
        }
    }
}
