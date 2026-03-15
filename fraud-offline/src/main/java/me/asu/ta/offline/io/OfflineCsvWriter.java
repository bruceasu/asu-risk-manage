package me.asu.ta.offline.io;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import me.asu.ta.Agg;
import me.asu.ta.BaselineStats;
import me.asu.ta.DetailRow;
import me.asu.ta.FxReplayWriter;
import me.asu.ta.LongSamples;
import me.asu.ta.OfflineAccountTracker;

public final class OfflineCsvWriter {
    public void writeDetail(Path out, List<DetailRow> rows) throws Exception {
        FxReplayWriter.writeDetail(out, rows);
    }

    public void writeAggAccountSymbol(Path out, Map<String, Agg> agg) throws Exception {
        FxReplayWriter.writeAggAccountSymbol(out, agg);
    }

    public BaselineStats writeAggAccount(Path out, Map<String, Agg> aggAccount, int minTrades, Agg global) throws Exception {
        return FxReplayWriter.writeAggAccount(out, aggAccount, minTrades, global);
    }

    public void writeBuckets(Path out, int bucketMin, String bucketBy, Map<String, Agg> buckets) throws Exception {
        FxReplayWriter.writeBuckets(out, bucketMin, bucketBy, buckets);
    }

    public void writeQuoteAgeStats(Path out, String scope, Map<String, LongSamples> samples) throws Exception {
        FxReplayWriter.writeQuoteAgeStats(out, scope, samples);
    }

    public void writeBaseline(Path out, BaselineStats baseline) throws Exception {
        FxReplayWriter.writeBaseline(out, baseline);
    }

    public void writeBotIndicators(Path out, Map<String, OfflineAccountTracker> trackers) throws Exception {
        FxReplayWriter.writeBotIndicators(out, trackers);
    }
}
