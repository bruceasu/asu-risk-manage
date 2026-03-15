package me.asu.ta.rule.library;

import java.time.Instant;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleSeverity;

public final class RuleLibraryTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-14T12:00:00Z");

    private RuleLibraryTestSupport() {
    }

    public static AccountFeatureSnapshot.Builder snapshotBuilder(String accountId) {
        return AccountFeatureSnapshot.builder(accountId, FIXED_TIME)
                .featureVersion(1)
                .accountAgeDays(180)
                .kycLevelNumeric(2)
                .registrationIpRiskScore(0.15d)
                .loginCount24h(8)
                .loginFailureCount24h(1)
                .loginFailureRate24h(0.10d)
                .uniqueIpCount24h(2)
                .highRiskIpLoginCount24h(0)
                .vpnIpLoginCount24h(0)
                .newDeviceLoginCount7d(0)
                .nightLoginRatio7d(0.12d)
                .transactionCount24h(4)
                .totalAmount24h(560.0d)
                .avgTransactionAmount24h(140.0d)
                .depositCount24h(2)
                .withdrawCount24h(1)
                .depositAmount24h(400.0d)
                .withdrawAmount24h(120.0d)
                .depositWithdrawRatio24h(0.30d)
                .uniqueCounterpartyCount24h(3)
                .withdrawAfterDepositDelayAvg24h(180.0d)
                .rapidWithdrawAfterDepositFlag24h(false)
                .rewardTransactionCount30d(0)
                .rewardWithdrawDelayAvg30d(72.0d)
                .uniqueDeviceCount7d(2)
                .deviceSwitchCount24h(0)
                .sharedDeviceAccounts7d(1)
                .securityEventCount24h(0)
                .rapidProfileChangeFlag24h(false)
                .securityChangeBeforeWithdrawFlag24h(false)
                .sharedIpAccounts7d(1)
                .sharedBankAccounts30d(1)
                .graphClusterSize30d(2)
                .riskNeighborCount30d(0)
                .anomalyScoreLast(0.08d);
    }

    public static RuleEvaluationContext context(AccountFeatureSnapshot snapshot) {
        return new RuleEvaluationContext(
                snapshot.accountId(),
                FIXED_TIME,
                snapshot.featureVersion(),
                EvaluationMode.REALTIME,
                Map.of(),
                Map.of("snapshot", snapshot));
    }

    public static RuleConfig config(String ruleCode, int version, int scoreWeight, RuleSeverity severity, Map<String, Object> parameters) {
        return new RuleConfig(
                ruleCode,
                version,
                scoreWeight,
                severity,
                parameters,
                null,
                FIXED_TIME.minusSeconds(300),
                null);
    }
}
