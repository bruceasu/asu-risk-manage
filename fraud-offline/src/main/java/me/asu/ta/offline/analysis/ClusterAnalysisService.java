package me.asu.ta.offline.analysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.AccountVec;
import me.asu.ta.Cluster;
import me.asu.ta.ClusterHelper;
import me.asu.ta.offline.ReplayCliOptions;
import me.asu.ta.offline.ReplayState;

public final class ClusterAnalysisService {
    public void writeClusters(ReplayCliOptions options, ReplayState state) throws Exception {
        ClusterHelper.clusterAccountsAndWrite(
                options.getOutputs().getCluster(),
                state.getAggByAccount(),
                options.getClusterK(),
                options.getClusterThreshold(),
                options.getMinTrades());
    }

    public Map<String, Integer> computeClusterSizes(ReplayCliOptions options, ReplayState state) {
        List<AccountVec> vecs = state.getAggByAccount().entrySet().stream()
                .filter(entry -> entry.getValue().n >= options.getMinTrades())
                .map(entry -> AccountVec.from(entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<Cluster> clusters = options.getClusterK() > 0
                ? ClusterHelper.kMeans(vecs, options.getClusterK(), 30)
                : ClusterHelper.thresholdClustering(vecs, options.getClusterThreshold());
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
