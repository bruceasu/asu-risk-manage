package me.asu.ta.offline.analysis;

import me.asu.ta.BaselineStats;
import me.asu.ta.offline.ReplayCliOptions;
import me.asu.ta.offline.ReplayState;

public final class BaselineAnalysisService {
    public BaselineStats computeBaseline(ReplayState state, ReplayCliOptions options) {
        if (options.isAggAccount() || options.isBaseline() || options.isReport()) {
            return new BaselineStats(state.getGlobalAgg());
        }
        return null;
    }
}
