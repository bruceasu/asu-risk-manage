package me.asu.ta.offline.integration;

import java.util.List;
import java.util.Map;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.offline.ReplayState;

public record OfflineAnalysisBundle(
        ReplayState replayState,
        BaselineStats baselineStats,
        List<Anomaly> anomalies,
        Map<String, Integer> clusterSizes
) {
}
