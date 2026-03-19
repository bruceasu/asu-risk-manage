package me.asu.ta.offline.analysis;

import me.asu.ta.offline.FxReplayCliOptions;
import me.asu.ta.offline.FxReplayEngine;
import me.asu.ta.offline.ReplayState;

public final class ReplayAnalysisService {
    public ReplayState replay(FxReplayCliOptions options) throws Exception {
        return FxReplayEngine.replay(options.getTradesPath(), options.getQuotesPath(), options.getReplay());
    }
}
