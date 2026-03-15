package me.asu.ta.rule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import javax.sql.DataSource;
import me.asu.ta.rule.RuleEngineCoreTestSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleSeverity;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.model.params.LoginRuleParams;
import me.asu.ta.rule.repository.RuleConfigReloadLogRepository;
import org.junit.jupiter.api.Test;

class RuleConfigServiceIntegrationTest {

    @Test
    void shouldReloadValidConfigsAndSkipMalformedOnes() throws Exception {
        DataSource dataSource = RuleEngineCoreTestSupport.createDataSource();
        RuleConfigReloadLogRepository reloadLogRepository = RuleEngineCoreTestSupport.ruleConfigReloadLogRepository(dataSource);
        RuleConfigService service = RuleEngineCoreTestSupport.ruleConfigService(dataSource);
        Instant now = Instant.now();
        Instant effectiveFrom = now.minusSeconds(3600);

        RuleEngineCoreTestSupport.insertRuleDefinition(dataSource, new RuleDefinition(
                "LOGIN_RULE_GOOD",
                "Login Rule Good",
                RuleCategory.LOGIN,
                "Valid login config",
                RuleSeverity.HIGH,
                "test",
                1,
                true,
                RuleEngineCoreTestSupport.FIXED_TIME,
                RuleEngineCoreTestSupport.FIXED_TIME));
        RuleEngineCoreTestSupport.insertRuleVersion(dataSource, new RuleVersion(
                "LOGIN_RULE_GOOD",
                1,
                "{\"minHighRiskIpLoginCount24h\":2}",
                35,
                true,
                effectiveFrom,
                null,
                RuleEngineCoreTestSupport.FIXED_TIME,
                "tester",
                "valid"));

        RuleEngineCoreTestSupport.insertRuleDefinition(dataSource, new RuleDefinition(
                "LOGIN_RULE_BAD",
                "Login Rule Bad",
                RuleCategory.LOGIN,
                "Invalid login config",
                RuleSeverity.HIGH,
                "test",
                1,
                true,
                RuleEngineCoreTestSupport.FIXED_TIME,
                RuleEngineCoreTestSupport.FIXED_TIME));
        RuleEngineCoreTestSupport.insertRuleVersion(dataSource, new RuleVersion(
                "LOGIN_RULE_BAD",
                1,
                "{\"unexpectedThreshold\":1}",
                35,
                true,
                effectiveFrom,
                null,
                RuleEngineCoreTestSupport.FIXED_TIME,
                "tester",
                "invalid"));

        service.reload();

        RuleConfig config = service.getConfig("LOGIN_RULE_GOOD", now).orElseThrow();
        assertEquals(2, config.parameters().get("minHighRiskIpLoginCount24h"));
        assertEquals(2, config.typedParameters(LoginRuleParams.class).orElseThrow().minHighRiskIpLoginCount24h());
        assertFalse(service.getConfig("LOGIN_RULE_BAD", now).isPresent());
        assertTrue(reloadLogRepository.findByRuleCode("PARTIAL_SUCCESS").isPresent());
        assertEquals(1, reloadLogRepository.findByRuleCode("PARTIAL_SUCCESS").orElseThrow().loadedRuleCount());
    }
}
