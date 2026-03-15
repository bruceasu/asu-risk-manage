package me.asu.ta.offline.integration;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public OfflineRiskBatchService(
            FeatureStoreService featureStoreService,
            RiskEngineFacade riskEngineFacade,
            OfflineSnapshotMappingService snapshotMappingService,
            OfflineGraphBridgeService graphBridgeService) {
        this.featureStoreService = featureStoreService;
        this.riskEngineFacade = riskEngineFacade;
        this.snapshotMappingService = snapshotMappingService;
        this.graphBridgeService = graphBridgeService;
    }

    public OfflineBatchIntegrationResult integrate(OfflineAnalysisBundle bundle) {
        Instant generatedAt = Instant.now();
        List<AccountFeatureSnapshot> snapshots = snapshotMappingService.mapSnapshots(bundle, generatedAt);
        for (AccountFeatureSnapshot snapshot : snapshots) {
            featureStoreService.persistSnapshot(snapshot);
            featureStoreService.persistHistory(snapshot);
        }

        Map<String, GraphRiskSignal> graphSignals = graphBridgeService.buildGraphSignals(bundle);
        List<RiskEvaluationRequest> requests = snapshots.stream()
                .map(snapshot -> new RiskEvaluationRequest(
                        snapshot.accountId(),
                        snapshot,
                        graphSignals.get(snapshot.accountId()),
                        snapshot.anomalyScoreLast() == null
                                ? null
                                : new MlAnomalySignal(
                                        snapshot.anomalyScoreLast(),
                                        snapshot.anomalyScoreLast(),
                                        "offline_replay_anomaly",
                                        generatedAt),
                        Map.of("source", "fraud-offline"),
                        EvaluationMode.BATCH,
                        generatedAt))
                .toList();
        Map<String, RiskScoreResult> riskResults = new LinkedHashMap<>(riskEngineFacade.evaluateBatch(requests));
        return new OfflineBatchIntegrationResult(List.copyOf(snapshots), Map.copyOf(riskResults));
    }
}
