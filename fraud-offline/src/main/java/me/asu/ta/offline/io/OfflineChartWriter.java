package me.asu.ta.offline.io;

import me.asu.ta.FxReplayCliOptions;
import me.asu.ta.ReplayState;

public final class OfflineChartWriter {
    public void writeDashboard(FxReplayCliOptions options, ReplayState state) throws Exception {
        me.asu.ta.util.Charts.writeDashboard(
                options.getOutputs().getChart(),
                state.getAggByAccount(),
                options.getMinTrades(),
                options.getChartTopN(),
                options.getTradesPath(),
                options.getQuotesPath());
    }
}
