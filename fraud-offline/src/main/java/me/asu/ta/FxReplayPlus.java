package me.asu.ta;

import me.asu.ta.offline.OfflineReplayCliApplication;

/**
 * 兼容旧入口，内部委派到新的离线 CLI 编排层。
 */
public class FxReplayPlus {
    public static void main(String[] args) throws Exception {
        new OfflineReplayCliApplication().run(args);
    }
}
