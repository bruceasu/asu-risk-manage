package me.asu.ta.offline.analysis;

import java.util.ArrayList;
import java.util.List;
import me.asu.ta.Agg;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.offline.ReplayState;

import static me.asu.ta.util.CommonUtils.mean;
import static me.asu.ta.util.CommonUtils.zscore;

public final class AnomalyAnalysisService {
    public List<Anomaly> computeAnomalies(ReplayState state, BaselineStats baselineStats, int minTrades) {
        List<Anomaly> anomalies = new ArrayList<>();
        for (var entry : state.getAggByAccount().entrySet()) {
            Agg agg = entry.getValue();
            if (agg.n < minTrades) {
                continue;
            }
            double mean500 = mean(agg.sumMark500, agg.n);
            double mean1s = mean(agg.sumMark1s, agg.n);
            double meanQA = mean(agg.sumQuoteAge, agg.n);
            anomalies.add(new Anomaly(
                    entry.getKey(),
                    zscore(mean500, baselineStats.mean500, baselineStats.std500),
                    zscore(mean1s, baselineStats.mean1s, baselineStats.std1s),
                    zscore(meanQA, baselineStats.meanQA, baselineStats.stdQA),
                    agg.n));
        }
        anomalies.sort((a, b) -> Double.compare(b.z500, a.z500));
        return anomalies;
    }
}
