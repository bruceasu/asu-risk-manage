package me.asu.ta.offline;

import me.asu.ta.FxReplayCliOptions;

public final class OfflineReportCliApplication {
    public static void main(String[] args) throws Exception {
        if (OfflineReplayCliSupport.hasHelpArg(args)) {
            OfflineReplayCliSupport.printHelp();
            return;
        }
        new OfflineReplayFacade().executeReportOnly(FxReplayCliOptions.fromArgs(args));
    }
}
