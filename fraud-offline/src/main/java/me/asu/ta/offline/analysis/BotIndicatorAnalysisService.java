package me.asu.ta.offline.analysis;

import me.asu.ta.DetailRow;
import me.asu.ta.IntervalStats;
import me.asu.ta.OfflineAccountTracker;
import me.asu.ta.ReplayState;

public final class BotIndicatorAnalysisService {
    public void enrichDetailRows(ReplayState state) {
        System.out.println("Computing bot indicators for " + state.getAccountTrackers().size() + " accounts...");
        for (DetailRow detailRow : state.getDetailRows()) {
            OfflineAccountTracker tracker = state.getAccountTrackers().get(detailRow.account);
            if (tracker == null) {
                continue;
            }
            IntervalStats stats = tracker.computeStats();
            detailRow.setBotIndicators(
                    stats,
                    tracker.getEntropy(),
                    tracker.getTPSLRatio(),
                    tracker.getClientIPCount(),
                    tracker.getClientTypes());
        }
    }
}
