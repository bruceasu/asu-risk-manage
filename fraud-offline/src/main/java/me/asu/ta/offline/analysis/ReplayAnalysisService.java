package me.asu.ta.offline.analysis;

import me.asu.ta.FxReplayCliOptions;
import me.asu.ta.FxReplayEngine;
import me.asu.ta.ReplayState;

public final class ReplayAnalysisService {
    public ReplayState replay(FxReplayCliOptions options) throws Exception {
        return FxReplayEngine.replay(options.getTradesPath(), options.getQuotesPath(), options.getReplay());
    }
}
