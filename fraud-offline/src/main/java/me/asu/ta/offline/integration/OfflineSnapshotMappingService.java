package me.asu.ta.offline.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.Agg;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.offline.OfflineAccountTracker;
import me.asu.ta.offline.ReplayState;

import org.springframework.stereotype.Service;

@Service
public class OfflineSnapshotMappingService {
    private static final int OFFLINE_FEATURE_VERSION = 9001;

    public List<AccountFeatureSnapshot> mapSnapshots(OfflineAnalysisBundle bundle, Instant generatedAt) {
        List<AccountFeatureSnapshot> snapshots = new ArrayList<>();
        ReplayState replayState = bundle.replayState();
        for (var entry : replayState.getAggByAccount().entrySet()) {
            String accountId = entry.getKey();
            Agg agg = entry.getValue();
            OfflineAccountTracker tracker = replayState.getAccountTrackers().get(accountId);
            int clusterSize = bundle.clusterSizes().getOrDefault(accountId, 1);
            snapshots.add(mapAccount(accountId, agg, tracker, clusterSize, generatedAt));
        }
        return snapshots;
    }

    AccountFeatureSnapshot mapAccount(
            String accountId,
            Agg agg,
            OfflineAccountTracker tracker,
            int clusterSize,
            Instant generatedAt) {
        AccountFeatureSnapshot.Builder builder = AccountFeatureSnapshot.builder(accountId, generatedAt)
                .featureVersion(OFFLINE_FEATURE_VERSION)
                .transactionCount24h(toInt(agg == null ? 0 : agg.n))
                .uniqueCounterpartyCount24h(agg == null ? null : agg.symbols.size())
                .graphClusterSize30d(clusterSize)
                .riskNeighborCount30d(Math.max(0, clusterSize - 1));

        if (tracker != null) {
            int uniqueIps = tracker.getClientIPCount();
            int uniqueClientTypes = countClientTypes(tracker.getClientTypes());
            if (uniqueIps > 0) {
                builder.uniqueIpCount24h(uniqueIps);
            }
            if (uniqueClientTypes > 0) {
                builder.uniqueDeviceCount7d(uniqueClientTypes);
                builder.deviceSwitchCount24h(Math.max(0, uniqueClientTypes - 1));
            }
        }
        return builder.build();
    }

    private int countClientTypes(String clientTypes) {
        if (clientTypes == null || clientTypes.isBlank()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(clientTypes.split("\\|"))
                .filter(value -> !value.isBlank())
                .count();
    }

    private Integer toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
