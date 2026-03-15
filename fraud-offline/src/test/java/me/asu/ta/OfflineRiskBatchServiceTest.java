package me.asu.ta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureStoreService;
import me.asu.ta.offline.integration.OfflineAnalysisBundle;
import me.asu.ta.offline.integration.OfflineBatchIntegrationResult;
import me.asu.ta.offline.integration.OfflineGraphBridgeService;
import me.asu.ta.offline.integration.OfflineRiskBatchService;
import me.asu.ta.offline.integration.OfflineSnapshotMappingService;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.junit.Assert;
import org.junit.Test;

public class OfflineRiskBatchServiceTest {
    @Test
    public void shouldPersistSnapshotsAndEvaluateRisk() {
        List<AccountFeatureSnapshot> persistedSnapshots = new ArrayList<>();
        List<AccountFeatureSnapshot> historySnapshots = new ArrayList<>();
        List<RiskEvaluationRequest> evaluatedRequests = new ArrayList<>();

        FeatureStoreService featureStoreService = new FeatureStoreService(null, null, List.of()) {
            @Override
            public int persistSnapshot(AccountFeatureSnapshot snapshot) {
                persistedSnapshots.add(snapshot);
                return 1;
            }

            @Override
            public int persistHistory(AccountFeatureSnapshot snapshot) {
                historySnapshots.add(snapshot);
                return 1;
            }
        };
        RiskEngineFacade riskEngineFacade = new RiskEngineFacade(null, null) {
            @Override
            public Map<String, RiskScoreResult> evaluateBatch(List<RiskEvaluationRequest> requests) {
                evaluatedRequests.addAll(requests);
                Map<String, RiskScoreResult> results = new LinkedHashMap<>();
                for (RiskEvaluationRequest request : requests) {
                    results.put(request.accountId(), new RiskScoreResult(
                            1L,
                            request.accountId(),
                            82.5d,
                            RiskLevel.HIGH,
                            "offline-profile",
                            request.snapshot().featureVersion(),
                            Instant.now(),
                            EvaluationMode.BATCH,
                            List.of("ML_ANOMALY_HIGH"),
                            new ScoreBreakdown(0, 70, 82.5d, 0, 82.5d, "offline-profile")));
                }
                return results;
            }
        };

        ReplayState state = new ReplayState();
        Agg agg = new Agg();
        agg.n = 10;
        state.getAggByAccount().put("A1", agg);
        OfflineAnalysisBundle bundle = new OfflineAnalysisBundle(
                state,
                null,
                List.of(new Anomaly("A1", 4.5d, 1.0d, 0.2d, 10)),
                Map.of("A1", 5));

        OfflineRiskBatchService service = new OfflineRiskBatchService(
                featureStoreService,
                riskEngineFacade,
                new OfflineSnapshotMappingService(),
                new OfflineGraphBridgeService());

        OfflineBatchIntegrationResult result = service.integrate(bundle);

        Assert.assertEquals(1, persistedSnapshots.size());
        Assert.assertEquals(1, historySnapshots.size());
        Assert.assertEquals(1, evaluatedRequests.size());
        Assert.assertEquals(1, result.snapshots().size());
        Assert.assertEquals(1, result.riskResults().size());
        GraphRiskSignal signal = evaluatedRequests.getFirst().graphRiskSignal();
        Assert.assertNotNull(signal);
        Assert.assertEquals(5, signal.graphClusterSize());
        Assert.assertEquals("A1", persistedSnapshots.getFirst().accountId());
    }
}
