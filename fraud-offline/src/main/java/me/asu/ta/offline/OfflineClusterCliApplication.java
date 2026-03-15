package me.asu.ta.offline;

import me.asu.ta.FxReplayCliOptions;

public final class OfflineClusterCliApplication {
    public static void main(String[] args) throws Exception {
        if (OfflineReplayCliSupport.hasHelpArg(args)) {
            OfflineReplayCliSupport.printHelp();
            return;
        }
        new OfflineReplayFacade().executeClusterOnly(FxReplayCliOptions.fromArgs(args));
    }
}
