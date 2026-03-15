package me.asu.ta.rule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import me.asu.ta.rule.RuleEngineCoreTestSupport;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.engine.RuleEngine;
import me.asu.ta.rule.engine.RuleRegistry;
import me.asu.ta.rule.engine.RuleResultAggregator;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleHitLog;
import me.asu.ta.rule.model.RuleSeverity;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import org.junit.jupiter.api.Test;

class RuleHitPersistenceIntegrationTest {

    @Test
    void shouldPersistRuleHitLogWithEvidenceAndEvaluationMode() throws Exception {
        DataSource dataSource = RuleEngineCoreTestSupport.createDataSource();
        RuleHitLogRepository hitLogRepository = RuleEngineCoreTestSupport.ruleHitLogRepository(dataSource);
        RuleConfigService configService = RuleEngineCoreTestSupport.ruleConfigService(dataSource);
        Instant now = Instant.now();
        Instant effectiveFrom = now.minusSeconds(3600);

        RuleEngineCoreTestSupport.insertRuleDefinition(dataSource, new RuleDefinition(
                "PERSIST_HIT_TEST",
                "Persist Hit Test",
                RuleCategory.SECURITY,
                "Persistence verification rule",
                RuleSeverity.MEDIUM,
                "test",
                1,
                true,
                RuleEngineCoreTestSupport.FIXED_TIME,
                RuleEngineCoreTestSupport.FIXED_TIME));
        RuleEngineCoreTestSupport.insertRuleVersion(dataSource, new RuleVersion(
                "PERSIST_HIT_TEST",
                1,
                "{}",
                15,
                true,
                effectiveFrom,
                null,
                RuleEngineCoreTestSupport.FIXED_TIME,
                "tester",
                "persist"));
        configService.reload();

        RuleEvaluationService evaluationService = new RuleEvaluationService(
                configService,
                new RuleEngine(new RuleRegistry(List.of(new PersistHitTestRule())), new RuleResultAggregator()),
                hitLogRepository,
                new ObjectMapper());

        RuleEvaluationContext context = new RuleEvaluationContext(
                "acct-hit-log",
                now,
                7,
                EvaluationMode.REALTIME,
                Map.of(),
                Map.of("snapshot", RuleEngineCoreTestSupport.snapshotBuilder("acct-hit-log").build()));

        evaluationService.evaluate(context);

        List<RuleHitLog> hitLogs = hitLogRepository.findByRuleCode("PERSIST_HIT_TEST");
        assertEquals(1, hitLogs.size());
        RuleHitLog hitLog = hitLogs.get(0);
        assertEquals("acct-hit-log", hitLog.accountId());
        assertEquals(7, hitLog.featureVersion());
        assertEquals(EvaluationMode.REALTIME, hitLog.evaluationMode());
        assertTrue(hitLog.evidenceJson().contains("\"signal\":\"persisted\""));
    }

    private static final class PersistHitTestRule implements FraudRule {
        @Override
        public String ruleCode() {
            return "PERSIST_HIT_TEST";
        }

        @Override
        public RuleCategory category() {
            return RuleCategory.SECURITY;
        }

        @Override
        public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
            return new RuleEvaluationResult(
                    ruleCode(),
                    config.version(),
                    true,
                    config.severity(),
                    config.scoreWeight(),
                    "PERSIST_HIT_TEST",
                    "Persistence test hit",
                    Map.of("signal", "persisted", "evaluatedAt", RuleEngineCoreTestSupport.FIXED_TIME.toString()));
        }
    }
}
