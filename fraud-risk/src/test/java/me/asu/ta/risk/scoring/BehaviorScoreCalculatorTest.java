package me.asu.ta.risk.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import org.junit.jupiter.api.Test;

class BehaviorScoreCalculatorTest {
    private final BehaviorScoreCalculator calculator = new BehaviorScoreCalculator();

    @Test
    void shouldScoreSnapshotBehaviorSignalsUsingSharedPolicy() {
        AccountFeatureSnapshot snapshot = RiskTestSupport.snapshotBuilder("acct-behavior-policy")
                .loginFailureRate24h(0.91d)
                .highRiskIpLoginCount24h(2)
                .withdrawAfterDepositDelayAvg24h(20.0d)
                .sharedDeviceAccounts7d(6)
                .securityChangeBeforeWithdrawFlag24h(true)
                .build();

        BehaviorRiskSignal signal = calculator.calculate(snapshot, Map.of());

        assertEquals(100.0d, signal.behaviorScore());
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_LOGIN_FAILURE_RATE_HIGH"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_HIGH_RISK_IP_ACTIVITY"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_RAPID_WITHDRAW_PATTERN"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_SHARED_DEVICE_EXPOSURE"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_SECURITY_CHANGE_BEFORE_WITHDRAW"));
    }

    @Test
    void shouldScoreOfflineBehaviorContextSignalsUsingSharedPolicy() {
        AccountFeatureSnapshot snapshot = RiskTestSupport.snapshotBuilder("acct-behavior-offline").build();

        BehaviorRiskSignal signal = calculator.calculate(
                snapshot,
                Map.of(
                        "behaviorClusterSize", 5,
                        "similarAccountCount", 4,
                        "coordinatedTradingScore", 72.0d));

        assertEquals(35.0d, signal.behaviorScore());
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_OFFLINE_CLUSTER_DENSE"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_OFFLINE_SIMILAR_ACCOUNTS"));
        assertTrue(signal.reasonCodes().contains("BEHAVIOR_OFFLINE_COORDINATED_TRADING"));
    }
}
