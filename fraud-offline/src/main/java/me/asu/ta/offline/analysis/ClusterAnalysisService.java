package me.asu.ta.offline.analysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.AccountVec;
import me.asu.ta.Cluster;
import me.asu.ta.FxReplayCliOptions;
import me.asu.ta.FxReplayClusterer;
import me.asu.ta.ReplayState;

public final class ClusterAnalysisService {
    public void writeClusters(FxReplayCliOptions options, ReplayState state) throws Exception {
        FxReplayClusterer.clusterAccountsAndWrite(
                options.getOutputs().getCluster(),
                state.getAggByAccount(),
                options.getClusterK(),
                options.getClusterThreshold(),
                options.getMinTrades());
    }

    public Map<String, Integer> computeClusterSizes(FxReplayCliOptions options, ReplayState state) {
        List<AccountVec> vecs = state.getAggByAccount().entrySet().stream()
                .filter(entry -> entry.getValue().n >= options.getMinTrades())
                .map(entry -> AccountVec.from(entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<Cluster> clusters = options.getClusterK() > 0
                ? FxReplayClusterer.kMeans(vecs, options.getClusterK(), 30)
                : FxReplayClusterer.thresholdClustering(vecs, options.getClusterThreshold());
        Map<String, Integer> clusterSizes = new LinkedHashMap<>();
        for (Cluster cluster : clusters) {
            int size = cluster.members.size();
            for (AccountVec member : cluster.members) {
                clusterSizes.put(member.accountId, size);
            }
        }
        for (String accountId : state.getAggByAccount().keySet()) {
            clusterSizes.putIfAbsent(accountId, 1);
        }
        return clusterSizes;
    }
}
