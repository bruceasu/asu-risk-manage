package me.asu.ta.offline.integration;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.Anomaly;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureStoreService;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.springframework.stereotype.Service;

@Service
public class OfflineRiskBatchService {
    private final FeatureStoreService featureStoreService;
    private final RiskEngineFacade riskEngineFacade;
    private final OfflineSnapshotMappingService snapshotMappingService;
    private final OfflineGraphBridgeService graphBridgeService;
    private final OfflineBehaviorRiskBridgeService behaviorRiskBridgeService;

    public OfflineRiskBatchService(
            FeatureStoreService featureStoreService,
            RiskEngineFacade riskEngineFacade,
            OfflineSnapshotMappingService snapshotMappingService,
            OfflineGraphBridgeService graphBridgeService,
            OfflineBehaviorRiskBridgeService behaviorRiskBridgeService) {
        this.featureStoreService = featureStoreService;
        this.riskEngineFacade = riskEngineFacade;
        this.snapshotMappingService = snapshotMappingService;
        this.graphBridgeService = graphBridgeService;
        this.behaviorRiskBridgeService = behaviorRiskBridgeService;
    }

    public OfflineBatchIntegrationResult integrate(OfflineAnalysisBundle bundle) {
        Instant generatedAt = Instant.now();
        List<AccountFeatureSnapshot> snapshots = snapshotMappingService.mapSnapshots(bundle, generatedAt);
        for (AccountFeatureSnapshot snapshot : snapshots) {
            featureStoreService.persistSnapshot(snapshot);
            featureStoreService.persistHistory(snapshot);
        }

        Map<String, GraphRiskSignal> graphSignals = graphBridgeService.buildGraphSignals(bundle);
        Map<String, MlAnomalySignal> anomalySignals = buildAnomalySignals(bundle.anomalies(), generatedAt);
        Map<String, Map<String, Object>> behaviorContextSignals = behaviorRiskBridgeService.buildContextSignals(bundle);
        List<RiskEvaluationRequest> requests = snapshots.stream()
                .map(snapshot -> new RiskEvaluationRequest(
                        snapshot.accountId(),
                        snapshot,
                        graphSignals.get(snapshot.accountId()),
                        anomalySignals.get(snapshot.accountId()),
                        mergeContextSignals(behaviorContextSignals.get(snapshot.accountId())),
                        EvaluationMode.BATCH,
                        generatedAt))
                .toList();
        Map<String, RiskScoreResult> riskResults = new LinkedHashMap<>(riskEngineFacade.evaluateBatch(requests));
        return new OfflineBatchIntegrationResult(List.copyOf(snapshots), Map.copyOf(riskResults));
    }

    private Map<String, MlAnomalySignal> buildAnomalySignals(List<Anomaly> anomalies, Instant generatedAt) {
        Map<String, MlAnomalySignal> signals = new HashMap<>();
        for (Anomaly anomaly : anomalies) {
            double raw = Math.max(Math.max(Math.max(0.0d, anomaly.z500), Math.max(0.0d, anomaly.z1s)), Math.max(0.0d, anomaly.zQA));
            double normalized = Math.min(100.0d, Math.round((raw / 6.0d) * 10000.0d) / 100.0d);
            signals.put(anomaly.account, new MlAnomalySignal(raw, normalized, "offline_replay_anomaly", generatedAt));
        }
        return signals;
    }

    private Map<String, Object> mergeContextSignals(Map<String, Object> behaviorSignals) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("source", "fraud-offline");
        if (behaviorSignals != null && !behaviorSignals.isEmpty()) {
            merged.putAll(behaviorSignals);
        }
        return Map.copyOf(merged);
    }
}
