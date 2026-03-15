package me.asu.ta.risk.reason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import javax.sql.DataSource;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import org.junit.jupiter.api.Test;

class RiskReasonGeneratorTest {
    @Test
    void shouldGenerateTopReasonCodesWithoutDuplicatesAndWithPriority() throws Exception {
        DataSource dataSource = RiskTestSupport.createDataSource();
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("RULE_TAKEOVER", RuleSeverity.CRITICAL, "RULE"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("GRAPH_SHARED_DEVICE_CLUSTER", RuleSeverity.HIGH, "GRAPH"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("ML_ANOMALY_HIGH", RuleSeverity.HIGH, "ML"));
        RiskTestSupport.insertReasonMapping(dataSource, RiskTestSupport.reasonMapping("BEHAVIOR_HIGH_RISK_IP_ACTIVITY", RuleSeverity.MEDIUM, "BEHAVIOR"));

        RiskReasonGenerator generator = new RiskReasonGenerator(RiskTestSupport.riskReasonMappingRepository(dataSource));
        RuleEngineResult ruleResult = new RuleEngineResult(
                "acct-risk-3",
                EvaluationMode.REALTIME,
                RiskTestSupport.FIXED_TIME,
                92,
                List.of(
                        new RuleEvaluationResult("RULE_A", 1, true, RuleSeverity.CRITICAL, 80, "RULE_TAKEOVER", "Account takeover pattern", java.util.Map.of()),
                        new RuleEvaluationResult("RULE_B", 1, true, RuleSeverity.HIGH, 60, "GRAPH_SHARED_DEVICE_CLUSTER", "Shared cluster", java.util.Map.of())),
                List.of("RULE_TAKEOVER", "GRAPH_SHARED_DEVICE_CLUSTER"));

        List<String> reasonCodes = generator.generateTopReasonCodes(
                ruleResult,
                new GraphRiskSignal(75.0d, 6, 4, 5, 1),
                new MlAnomalySignal(0.88d, 88.0d, "risk-model", RiskTestSupport.FIXED_TIME),
                new BehaviorRiskSignal(20.0d, java.util.Map.of(), List.of("BEHAVIOR_HIGH_RISK_IP_ACTIVITY")),
                5);

        assertEquals("GRAPH_HIGH_RISK_NEIGHBORS", reasonCodes.getFirst());
        assertEquals("GRAPH_SHARED_DEVICE_CLUSTER", reasonCodes.get(1));
        assertFalse(reasonCodes.stream().filter("GRAPH_SHARED_DEVICE_CLUSTER"::equals).skip(1).findAny().isPresent());
        assertEquals(List.of(
                "GRAPH_HIGH_RISK_NEIGHBORS",
                "GRAPH_SHARED_DEVICE_CLUSTER",
                "ML_ANOMALY_HIGH",
                "GRAPH_LARGE_CLUSTER",
                "BEHAVIOR_HIGH_RISK_IP_ACTIVITY"), reasonCodes);
    }
}
