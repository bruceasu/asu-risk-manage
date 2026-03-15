package me.asu.ta.offline.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.Agg;
import me.asu.ta.Anomaly;
import me.asu.ta.OfflineAccountTracker;
import me.asu.ta.ReplayState;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.stereotype.Service;

@Service
public class OfflineSnapshotMappingService {
    private static final int OFFLINE_FEATURE_VERSION = 9001;

    public List<AccountFeatureSnapshot> mapSnapshots(OfflineAnalysisBundle bundle, Instant generatedAt) {
        Map<String, Anomaly> anomalyByAccount = new HashMap<>();
        for (Anomaly anomaly : bundle.anomalies()) {
            anomalyByAccount.put(anomaly.account, anomaly);
        }

        List<AccountFeatureSnapshot> snapshots = new ArrayList<>();
        ReplayState replayState = bundle.replayState();
        for (var entry : replayState.getAggByAccount().entrySet()) {
            String accountId = entry.getKey();
            Agg agg = entry.getValue();
            OfflineAccountTracker tracker = replayState.getAccountTrackers().get(accountId);
            int clusterSize = bundle.clusterSizes().getOrDefault(accountId, 1);
            snapshots.add(mapAccount(accountId, agg, tracker, anomalyByAccount.get(accountId), clusterSize, generatedAt));
        }
        return snapshots;
    }

    AccountFeatureSnapshot mapAccount(
            String accountId,
            Agg agg,
            OfflineAccountTracker tracker,
            Anomaly anomaly,
            int clusterSize,
            Instant generatedAt) {
        AccountFeatureSnapshot.Builder builder = AccountFeatureSnapshot.builder(accountId, generatedAt)
                .featureVersion(OFFLINE_FEATURE_VERSION)
                .transactionCount24h(toInt(agg == null ? 0 : agg.n))
                .uniqueCounterpartyCount24h(agg == null ? null : agg.symbols.size())
                .graphClusterSize30d(clusterSize)
                .riskNeighborCount30d(Math.max(0, clusterSize - 1))
                .anomalyScoreLast(normalizeAnomaly(anomaly));

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

    private Double normalizeAnomaly(Anomaly anomaly) {
        if (anomaly == null) {
            return null;
        }
        double peak = Math.max(Math.max(Math.max(0.0d, anomaly.z500), Math.max(0.0d, anomaly.z1s)), Math.max(0.0d, anomaly.zQA));
        return Math.min(100.0d, Math.round((peak / 6.0d) * 10000.0d) / 100.0d);
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
