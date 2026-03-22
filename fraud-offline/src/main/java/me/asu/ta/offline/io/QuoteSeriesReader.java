package me.asu.ta.offline.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.csvreader.CsvReader;

import me.asu.ta.QuoteSeries;
import me.asu.ta.dto.QuoteEvent;
import me.asu.ta.util.CommonUtils;

public class QuoteSeriesReader {
    static final String COL_Q_SYM = "symbol";
    static final String COL_Q_T = "quote_time_ms";
    static final String COL_BID = "bid";
    static final String COL_ASK = "ask";
    Map<String, QuoteSeries> quotesBySymbol = new HashMap<>();

    public QuoteSeriesReader(Path quotesCsv) throws IOException {
        add(quotesCsv);
    }

    public QuoteSeries get(String symbol) {
        return quotesBySymbol.get(symbol);
    }

    public void add(Path path) throws IOException {
         Map<String, QuoteSeries> m = loadQuotes(path);
         quotesBySymbol.putAll(m);
    }

    private static Map<String, QuoteSeries> loadQuotes(Path quotesCsv) throws IOException {
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
                Long quoteTime = CommonUtils.parseLongSafe(csv.get(COL_Q_T).trim());
                Double bid = CommonUtils.parseDoubleSafe(csv.get(COL_BID).trim());
                Double ask = CommonUtils.parseDoubleSafe(csv.get(COL_ASK).trim());
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
}
