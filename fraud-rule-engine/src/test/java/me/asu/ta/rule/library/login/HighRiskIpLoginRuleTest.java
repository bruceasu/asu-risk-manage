package me.asu.ta.rule.library.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.library.RuleLibraryTestSupport;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import org.junit.jupiter.api.Test;

class HighRiskIpLoginRuleTest {
    private final HighRiskIpLoginRule rule = new HighRiskIpLoginRule();

    @Test
    void shouldHitWhenHighRiskIpLoginCountReachesThreshold() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-risk-1")
                .highRiskIpLoginCount24h(3)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "HIGH_RISK_IP_LOGIN", 1, 35, RuleSeverity.HIGH, Map.of("minHighRiskIpLoginCount24h", 2));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertTrue(result.hit());
        assertEquals(35, result.score());
        assertEquals("HIGH_RISK_IP_LOGIN", result.reasonCode());
    }

    @Test
    void shouldNotHitWhenHighRiskIpLoginCountStaysBelowThreshold() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-risk-2")
                .highRiskIpLoginCount24h(1)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "HIGH_RISK_IP_LOGIN", 1, 35, RuleSeverity.HIGH, Map.of("minHighRiskIpLoginCount24h", 2));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertFalse(result.hit());
        assertEquals(0, result.score());
    }
}
