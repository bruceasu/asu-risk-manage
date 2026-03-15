package me.asu.ta.rule.library.composite;

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

class AtoSuspicionRuleTest {
    private final AtoSuspicionRule rule = new AtoSuspicionRule();

    @Test
    void shouldHitWhenAllAtoSignalsArePresent() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-ato-1")
                .newDeviceLoginCount7d(2)
                .highRiskIpLoginCount24h(2)
                .securityChangeBeforeWithdrawFlag24h(true)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "ATO_SUSPICION_COMPOSITE",
                1,
                60,
                RuleSeverity.CRITICAL,
                Map.of(
                        "minNewDeviceLoginCount7d", 1,
                        "minHighRiskIpLoginCount24h", 1,
                        "requireSecurityChangeBeforeWithdrawFlag", true));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertTrue(result.hit());
        assertEquals(60, result.score());
        assertEquals("ATO_SUSPICION_COMPOSITE", result.reasonCode());
    }

    @Test
    void shouldNotHitWhenRequiredSecurityChangeSignalIsMissing() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-ato-2")
                .newDeviceLoginCount7d(2)
                .highRiskIpLoginCount24h(2)
                .securityChangeBeforeWithdrawFlag24h(false)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "ATO_SUSPICION_COMPOSITE",
                1,
                60,
                RuleSeverity.CRITICAL,
                Map.of(
                        "minNewDeviceLoginCount7d", 1,
                        "minHighRiskIpLoginCount24h", 1,
                        "requireSecurityChangeBeforeWithdrawFlag", true));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertFalse(result.hit());
        assertEquals(0, result.score());
    }
}
