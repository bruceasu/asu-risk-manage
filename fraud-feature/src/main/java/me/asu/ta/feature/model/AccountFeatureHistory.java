package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Historical feature snapshot mapped to the {@code account_feature_history} table.
 *
 * @param snapshotId account_feature_history.snapshot_id
 * @param snapshotTime account_feature_history.snapshot_time
 * @param accountId account_feature_history.account_id
 * @param featureVersion account_feature_history.feature_version
 * @param accountAgeDays account_feature_history.account_age_days
 * @param kycLevelNumeric account_feature_history.kyc_level_numeric
 * @param registrationIpRiskScore account_feature_history.registration_ip_risk_score
 * @param loginCount24h account_feature_history.login_count_24h
 * @param loginFailureCount24h account_feature_history.login_failure_count_24h
 * @param loginFailureRate24h account_feature_history.login_failure_rate_24h
 * @param uniqueIpCount24h account_feature_history.unique_ip_count_24h
 * @param highRiskIpLoginCount24h account_feature_history.high_risk_ip_login_count_24h
 * @param vpnIpLoginCount24h account_feature_history.vpn_ip_login_count_24h
 * @param newDeviceLoginCount7d account_feature_history.new_device_login_count_7d
 * @param nightLoginRatio7d account_feature_history.night_login_ratio_7d
 * @param transactionCount24h account_feature_history.transaction_count_24h
 * @param totalAmount24h account_feature_history.total_amount_24h
 * @param avgTransactionAmount24h account_feature_history.avg_transaction_amount_24h
 * @param depositCount24h account_feature_history.deposit_count_24h
 * @param withdrawCount24h account_feature_history.withdraw_count_24h
 * @param depositAmount24h account_feature_history.deposit_amount_24h
 * @param withdrawAmount24h account_feature_history.withdraw_amount_24h
 * @param depositWithdrawRatio24h account_feature_history.deposit_withdraw_ratio_24h
 * @param uniqueCounterpartyCount24h account_feature_history.unique_counterparty_count_24h
 * @param withdrawAfterDepositDelayAvg24h account_feature_history.withdraw_after_deposit_delay_avg_24h
 * @param rapidWithdrawAfterDepositFlag24h account_feature_history.rapid_withdraw_after_deposit_flag_24h
 * @param rewardTransactionCount30d account_feature_history.reward_transaction_count_30d
 * @param rewardWithdrawDelayAvg30d account_feature_history.reward_withdraw_delay_avg_30d
 * @param uniqueDeviceCount7d account_feature_history.unique_device_count_7d
 * @param deviceSwitchCount24h account_feature_history.device_switch_count_24h
 * @param sharedDeviceAccounts7d account_feature_history.shared_device_accounts_7d
 * @param securityEventCount24h account_feature_history.security_event_count_24h
 * @param rapidProfileChangeFlag24h account_feature_history.rapid_profile_change_flag_24h
 * @param securityChangeBeforeWithdrawFlag24h account_feature_history.security_change_before_withdraw_flag_24h
 * @param sharedIpAccounts7d account_feature_history.shared_ip_accounts_7d
 * @param sharedBankAccounts30d account_feature_history.shared_bank_accounts_30d
 * @param graphClusterSize30d account_feature_history.graph_cluster_size_30d
 * @param riskNeighborCount30d account_feature_history.risk_neighbor_count_30d
 * @param anomalyScoreLast account_feature_history.anomaly_score_last
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
        Integer riskNeighborCount30d,
        Double anomalyScoreLast
) {
    public AccountFeatureHistory {
        Objects.requireNonNull(snapshotTime, "snapshotTime");
        Objects.requireNonNull(accountId, "accountId");
    }
}
