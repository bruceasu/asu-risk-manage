package me.asu.ta.rule.library.transaction;

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

class RapidWithdrawAfterDepositRuleTest {
    private final RapidWithdrawAfterDepositRule rule = new RapidWithdrawAfterDepositRule();

    @Test
    void shouldHitWhenWithdrawDelayIsShortAfterDeposits() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-txn-1")
                .depositCount24h(2)
                .withdrawCount24h(2)
                .withdrawAfterDepositDelayAvg24h(12.0d)
                .rapidWithdrawAfterDepositFlag24h(false)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "RAPID_WITHDRAW_AFTER_DEPOSIT",
                1,
                45,
                RuleSeverity.HIGH,
                Map.of(
                        "maxDelayMinutes", 30,
                        "minDepositCount24h", 1,
                        "minWithdrawCount24h", 1));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertTrue(result.hit());
        assertEquals(45, result.score());
    }

    @Test
    void shouldNotHitWhenDelayIsTooLongAndRapidFlagIsFalse() {
        AccountFeatureSnapshot snapshot = RuleLibraryTestSupport.snapshotBuilder("acct-txn-2")
                .depositCount24h(2)
                .withdrawCount24h(1)
                .withdrawAfterDepositDelayAvg24h(90.0d)
                .rapidWithdrawAfterDepositFlag24h(false)
                .build();
        RuleConfig config = RuleLibraryTestSupport.config(
                "RAPID_WITHDRAW_AFTER_DEPOSIT",
                1,
                45,
                RuleSeverity.HIGH,
                Map.of(
                        "maxDelayMinutes", 30,
                        "minDepositCount24h", 1,
                        "minWithdrawCount24h", 1));

        RuleEvaluationResult result = rule.evaluate(RuleLibraryTestSupport.context(snapshot), config);

        assertFalse(result.hit());
        assertEquals(0, result.score());
    }
}
