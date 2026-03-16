package me.asu.ta.risk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.repository.RiskScoreResultRepository;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import org.junit.jupiter.api.Test;

class RiskEvaluationServiceIntegrationTest {
    @Test
    void shouldEvaluateBatchRiskAndPersistResults() throws Exception {
        DataSource dataSource = RiskTestSupport.createDataSource();
        RiskTestSupport.insertWeightProfile(dataSource, RiskTestSupport.defaultProfile());
        RiskTestSupport.insertWeightProfile(dataSource, RiskTestSupport.noMlProfile());
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("RULE_TAKEOVER", RuleSeverity.CRITICAL, "RULE"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("RULE_WALLET_DRAIN", RuleSeverity.HIGH, "RULE"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_HIGH_RISK_IP_ACTIVITY", RuleSeverity.MEDIUM, "BEHAVIOR"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_SHARED_DEVICE_EXPOSURE", RuleSeverity.MEDIUM, "BEHAVIOR"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_OFFLINE_CLUSTER_DENSE", RuleSeverity.MEDIUM, "BEHAVIOR"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_OFFLINE_SIMILAR_ACCOUNTS", RuleSeverity.MEDIUM, "BEHAVIOR"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_OFFLINE_COORDINATED_TRADING", RuleSeverity.HIGH, "BEHAVIOR"));

        RiskEvaluationService service = RiskTestSupport.riskEvaluationService(dataSource);

        AccountFeatureSnapshot firstSnapshot = RiskTestSupport.snapshotBuilder("acct-batch-1")
                .highRiskIpLoginCount24h(2)
                .sharedDeviceAccounts7d(6)
                .securityChangeBeforeWithdrawFlag24h(true)
                .riskNeighborCount30d(4)
                .graphClusterSize30d(6)
                .build();
        AccountFeatureSnapshot secondSnapshot = RiskTestSupport.snapshotBuilder("acct-batch-2")
                .loginFailureRate24h(0.18d)
                .highRiskIpLoginCount24h(0)
                .sharedDeviceAccounts7d(1)
                .graphClusterSize30d(1)
                .riskNeighborCount30d(0)
                .build();

        List<RiskEvaluationRequest> requests = List.of(
                new RiskEvaluationRequest(
                        "acct-batch-1",
                        firstSnapshot,
                        null,
                        new MlAnomalySignal(0.91d, 91.0d, "test-anomaly", RiskTestSupport.FIXED_TIME),
                        Map.of(
                                "channel", "web",
                                "behaviorClusterSize", 5,
                                "similarAccountCount", 4,
                                "coordinatedTradingScore", 72.0d),
                        EvaluationMode.BATCH,
                        RiskTestSupport.FIXED_TIME),
                new RiskEvaluationRequest("acct-batch-2", secondSnapshot, new GraphRiskSignal(10.0d, 1, 0, 1, 0), null, Map.of("channel", "app"), EvaluationMode.BATCH, RiskTestSupport.FIXED_TIME));

        Map<String, RuleEngineResult> ruleResults = Map.of(
                "acct-batch-1",
                new RuleEngineResult(
                        "acct-batch-1",
                        EvaluationMode.BATCH,
                        RiskTestSupport.FIXED_TIME,
                        85,
                        List.of(new RuleEvaluationResult("ATO_RULE", 3, true, RuleSeverity.CRITICAL, 85, "RULE_TAKEOVER", "Takeover indicators", Map.of("device", "shared"))),
                        List.of("RULE_TAKEOVER")),
                "acct-batch-2",
                new RuleEngineResult(
                        "acct-batch-2",
                        EvaluationMode.BATCH,
                        RiskTestSupport.FIXED_TIME,
                        22,
                        List.of(new RuleEvaluationResult("WATCH_RULE", 1, true, RuleSeverity.HIGH, 22, "RULE_WALLET_DRAIN", "Withdrawal watchlist", Map.of())),
                        List.of("RULE_WALLET_DRAIN")));

        Map<String, RiskScoreResult> results = service.evaluateBatchRisk(requests, ruleResults);

        assertEquals(2, results.size());
        assertEquals(RiskLevel.CRITICAL, results.get("acct-batch-1").riskLevel());
        assertEquals("DEFAULT", results.get("acct-batch-1").profileName());
        assertEquals("GRAPH_HIGH_RISK_NEIGHBORS", results.get("acct-batch-1").topReasonCodes().getFirst());
        assertTrue(results.get("acct-batch-1").topReasonCodes().contains("BEHAVIOR_OFFLINE_COORDINATED_TRADING"));
        assertEquals(EvaluationMode.BATCH, results.get("acct-batch-1").evaluationMode());

        assertEquals("NO_ML", results.get("acct-batch-2").profileName());
        assertEquals(EvaluationMode.BATCH, results.get("acct-batch-2").evaluationMode());
        assertTrue(results.get("acct-batch-2").riskScore() >= 0.0d);

        RiskScoreResultRepository repository = RiskTestSupport.riskScoreResultRepository(dataSource);
        assertTrue(repository.findLatestRiskScoreByAccountId("acct-batch-1").isPresent());
        assertTrue(repository.findLatestRiskScoreByAccountId("acct-batch-2").isPresent());
    }
}
