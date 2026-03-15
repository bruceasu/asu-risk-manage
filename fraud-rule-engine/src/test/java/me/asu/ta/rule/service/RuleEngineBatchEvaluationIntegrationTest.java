package me.asu.ta.rule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.RuleEngineCoreTestSupport;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.engine.RuleEngine;
import me.asu.ta.rule.engine.RuleRegistry;
import me.asu.ta.rule.engine.RuleResultAggregator;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleEngineFacadeContext;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import org.junit.jupiter.api.Test;

class RuleEngineBatchEvaluationIntegrationTest {

    @Test
    void shouldEvaluateBatchAccountsAndPersistOnlyHits() throws Exception {
        DataSource dataSource = RuleEngineCoreTestSupport.createDataSource();
        RuleHitLogRepository hitLogRepository = RuleEngineCoreTestSupport.ruleHitLogRepository(dataSource);
        RuleConfigService configService = RuleEngineCoreTestSupport.ruleConfigService(dataSource);
        Instant now = Instant.now();
        Instant effectiveFrom = now.minusSeconds(3600);

        RuleEngineCoreTestSupport.insertRuleDefinition(dataSource, new RuleDefinition(
                "BATCH_LOGIN_TEST",
                "Batch Login Test",
                RuleCategory.LOGIN,
                "Batch evaluation test rule",
                RuleSeverity.HIGH,
                "test",
                1,
                true,
                RuleEngineCoreTestSupport.FIXED_TIME,
                RuleEngineCoreTestSupport.FIXED_TIME));
        RuleEngineCoreTestSupport.insertRuleVersion(dataSource, new RuleVersion(
                "BATCH_LOGIN_TEST",
                1,
                "{\"minHighRiskIpLoginCount24h\":2}",
                25,
                true,
                effectiveFrom,
                null,
                RuleEngineCoreTestSupport.FIXED_TIME,
                "tester",
                "batch"));
        configService.reload();

        RuleRegistry ruleRegistry = new RuleRegistry(List.of(new BatchLoginTestRule()));
        RuleEvaluationService evaluationService = new RuleEvaluationService(
                configService,
                new RuleEngine(ruleRegistry, new RuleResultAggregator()),
                hitLogRepository,
                new ObjectMapper());
        RuleEngineFacade facade = new RuleEngineFacade(evaluationService);

        AccountFeatureSnapshot hitSnapshot = RuleEngineCoreTestSupport.snapshotBuilder("acct-batch-hit")
                .highRiskIpLoginCount24h(3)
                .build();
        AccountFeatureSnapshot missSnapshot = RuleEngineCoreTestSupport.snapshotBuilder("acct-batch-miss")
                .highRiskIpLoginCount24h(0)
                .build();

        Map<String, RuleEngineResult> results = facade.evaluateBatch(
                List.of("acct-batch-hit", "acct-batch-miss"),
                Map.of(
                        "acct-batch-hit", new RuleEngineFacadeContext(hitSnapshot, now, null, Map.of(), Map.of()),
                        "acct-batch-miss", new RuleEngineFacadeContext(missSnapshot, now, null, Map.of(), Map.of())));

        assertEquals(2, results.size());
        assertEquals(25, results.get("acct-batch-hit").totalScore());
        assertTrue(results.get("acct-batch-hit").hits().stream().anyMatch(hit -> hit.ruleCode().equals("BATCH_LOGIN_TEST")));
        assertEquals(0, results.get("acct-batch-miss").totalScore());
        assertEquals(1, hitLogRepository.findByRuleCode("BATCH_LOGIN_TEST").size());
        assertEquals("acct-batch-hit", hitLogRepository.findByRuleCode("BATCH_LOGIN_TEST").get(0).accountId());
    }

    private static final class BatchLoginTestRule implements FraudRule {
        @Override
        public String ruleCode() {
            return "BATCH_LOGIN_TEST";
        }

        @Override
        public RuleCategory category() {
            return RuleCategory.LOGIN;
        }

        @Override
        public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
            AccountFeatureSnapshot snapshot = (AccountFeatureSnapshot) context.attributes().get("snapshot");
            int threshold = ((Number) config.parameters().get("minHighRiskIpLoginCount24h")).intValue();
            boolean hit = snapshot.highRiskIpLoginCount24h() != null && snapshot.highRiskIpLoginCount24h() >= threshold;
            return new RuleEvaluationResult(
                    ruleCode(),
                    config.version(),
                    hit,
                    config.severity(),
                    hit ? config.scoreWeight() : 0,
                    "BATCH_LOGIN_TEST",
                    hit ? "Batch login test hit" : "Batch login test miss",
                    Map.of("highRiskIpLoginCount24h", snapshot.highRiskIpLoginCount24h(), "threshold", threshold));
        }
    }
}
