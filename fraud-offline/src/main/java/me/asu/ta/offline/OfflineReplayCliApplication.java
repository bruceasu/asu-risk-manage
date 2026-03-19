package me.asu.ta.offline;

public final class OfflineReplayCliApplication {
    private final OfflineReplayFacade facade;

    public OfflineReplayCliApplication() {
        this(new OfflineReplayFacade());
    }

    OfflineReplayCliApplication(OfflineReplayFacade facade) {
        this.facade = facade;
    }

    public static void main(String[] args) throws Exception {
        new OfflineReplayCliApplication().run(args);
    }

    public void run(String[] args) throws Exception {
        if (OfflineReplayCliSupport.hasHelpArg(args)) {
            OfflineReplayCliSupport.printHelp();
            return;
        }
        facade.execute(FxReplayCliOptions.fromArgs(args));
    }
}
