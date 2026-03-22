package me.asu.ta;

public class DetailRow {
    public static final long[] DELTAS_MS = {100, 500, 1000, 5000};
    public static final String[] DELTA_NAMES = {"100ms", "500ms", "1s", "5s"};

    public final String account;
    public final String symbol;
    public final String side;
    public final long execTimeMs;
    public final double volume;
    public final double mid0;
    public final long lastQuoteT0;
    public final long quoteAgeMs;
    public final Double[] mids = new Double[DELTAS_MS.length];
    public final Double[] marks = new Double[DELTAS_MS.length];

    public Double cv;
    public Integer botScore;
    public Double entropy;
    public Double tpslRatio;
    public Integer clientIPCount;
    public String clientTypes;

    public DetailRow(String account, String symbol, String side, long execTimeMs, double volume,
            double mid0, long lastQuoteT0, long quoteAgeMs) {
        this.account = account;
        this.symbol = symbol;
        this.side = side;
        this.execTimeMs = execTimeMs;
        this.volume = volume;
        this.mid0 = mid0;
        this.lastQuoteT0 = lastQuoteT0;
        this.quoteAgeMs = quoteAgeMs;
    }

    public void setBotIndicators(IntervalStats stats, double entropy, double tpslRatio, int ipCount, String types) {
        if (stats != null) {
            this.cv = stats.cv();
            this.botScore = stats.isBotLike()
                    ? Math.min(100, (int) (stats.cv() < 0.10 ? 95 : stats.cv() < 0.15 ? 85 : 70))
                    : 0;
        }
        this.entropy = entropy;
        this.tpslRatio = tpslRatio;
        this.clientIPCount = ipCount;
        this.clientTypes = types;
    }
}
