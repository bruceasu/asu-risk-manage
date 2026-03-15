package me.asu.ta.rule.library.device;

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

class SharedDeviceClusterRuleTest {
    private final SharedDeviceClusterRule rule = new SharedDeviceClusterRule();

    @Test
    void shouldHitWhenSharedDeviceAccountCountReachesThreshold() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-device-1")
                .sharedDeviceAccounts7d(7)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "SHARED_DEVICE_CLUSTER", 1, 25, RuleSeverity.MEDIUM, Map.of("minSharedDeviceAccounts7d", 5));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertTrue(result.hit());
        assertEquals(25, result.score());
    }

    @Test
    void shouldNotHitWhenSharedDeviceAccountCountIsBelowThreshold() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-device-2")
                .sharedDeviceAccounts7d(3)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "SHARED_DEVICE_CLUSTER", 1, 25, RuleSeverity.MEDIUM, Map.of("minSharedDeviceAccounts7d", 5));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertFalse(result.hit());
        assertEquals(0, result.score());
    }
}
