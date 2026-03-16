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

class OfflineCoordinatedTradingRuleTest {
    private final OfflineCoordinatedTradingRule rule = new OfflineCoordinatedTradingRule();

    @Test
    void shouldHitWhenOfflineBehaviorContextExceedsThresholds() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-offline-rule-hit").build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "OFFLINE_COORDINATED_TRADING",
                1,
                18,
                RuleSeverity.MEDIUM,
                Map.of(
                        "minCoordinatedTradingScore", 60.0d,
                        "minSimilarAccountCount", 3,
                        "minBehaviorClusterSize", 4));

        RuleEvaluationResult result = rule.evaluate(
                RuleLibraryTestSupport.context(snapshot, RuleLibraryTestSupport.behaviorContextSignals()),
                config);

        assertTrue(result.hit());
        assertEquals(18, result.score());
        assertEquals("OFFLINE_COORDINATED_TRADING_SIGNAL", result.reasonCode());
    }

    @Test
    void shouldNotHitWhenOfflineBehaviorContextIsMissing() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-offline-rule-miss").build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "OFFLINE_COORDINATED_TRADING",
                1,
                18,
                RuleSeverity.MEDIUM,
                Map.of(
                        "minCoordinatedTradingScore", 60.0d,
                        "minSimilarAccountCount", 3,
                        "minBehaviorClusterSize", 4));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertFalse(result.hit());
        assertEquals(0, result.score());
    }
}
