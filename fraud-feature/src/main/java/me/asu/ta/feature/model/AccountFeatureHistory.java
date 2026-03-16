package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Historical feature snapshot mapped to the {@code account_feature_history} table.
 */
public record AccountFeatureHistory(
        long snapshotId,
        Instant snapshotTime,
        String accountId,
        int featureVersion,
        Integer accountAgeDays,
        Integer kycLevelNumeric,
        Double registrationIpRiskScore,
        Integer loginCount24h,
        Integer loginFailureCount24h,
        Double loginFailureRate24h,
        Integer uniqueIpCount24h,
        Integer highRiskIpLoginCount24h,
        Integer vpnIpLoginCount24h,
        Integer newDeviceLoginCount7d,
        Double nightLoginRatio7d,
        Integer transactionCount24h,
        Double totalAmount24h,
        Double avgTransactionAmount24h,
        Integer depositCount24h,
        Integer withdrawCount24h,
        Double depositAmount24h,
        Double withdrawAmount24h,
        Double depositWithdrawRatio24h,
        Integer uniqueCounterpartyCount24h,
        Double withdrawAfterDepositDelayAvg24h,
        Boolean rapidWithdrawAfterDepositFlag24h,
        Integer rewardTransactionCount30d,
        Double rewardWithdrawDelayAvg30d,
        Integer uniqueDeviceCount7d,
        Integer deviceSwitchCount24h,
        Integer sharedDeviceAccounts7d,
        Integer securityEventCount24h,
        Boolean rapidProfileChangeFlag24h,
        Boolean securityChangeBeforeWithdrawFlag24h,
        Integer sharedIpAccounts7d,
        Integer sharedBankAccounts30d,
        Integer graphClusterSize30d,
        Integer riskNeighborCount30d
) {
    public AccountFeatureHistory {
        Objects.requireNonNull(snapshotTime, "snapshotTime");
        Objects.requireNonNull(accountId, "accountId");
    }
}
