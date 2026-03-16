package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Current feature snapshot mapped to the {@code account_feature_snapshot} table.
 */
public final class AccountFeatureSnapshot {
    /** account_feature_snapshot.account_id */
    private final String accountId;
    /** account_feature_snapshot.feature_version */
    private final int featureVersion;
    /** account_feature_snapshot.generated_at */
    private final Instant generatedAt;
    /** account_feature_snapshot.account_age_days */
    private final Integer accountAgeDays;
    /** account_feature_snapshot.kyc_level_numeric */
    private final Integer kycLevelNumeric;
    /** account_feature_snapshot.registration_ip_risk_score */
    private final Double registrationIpRiskScore;
    /** account_feature_snapshot.login_count_24h */
    private final Integer loginCount24h;
    /** account_feature_snapshot.login_failure_count_24h */
    private final Integer loginFailureCount24h;
    /** account_feature_snapshot.login_failure_rate_24h */
    private final Double loginFailureRate24h;
    /** account_feature_snapshot.unique_ip_count_24h */
    private final Integer uniqueIpCount24h;
    /** account_feature_snapshot.high_risk_ip_login_count_24h */
    private final Integer highRiskIpLoginCount24h;
    /** account_feature_snapshot.vpn_ip_login_count_24h */
    private final Integer vpnIpLoginCount24h;
    /** account_feature_snapshot.new_device_login_count_7d */
    private final Integer newDeviceLoginCount7d;
    /** account_feature_snapshot.night_login_ratio_7d */
    private final Double nightLoginRatio7d;
    /** account_feature_snapshot.transaction_count_24h */
    private final Integer transactionCount24h;
    /** account_feature_snapshot.total_amount_24h */
    private final Double totalAmount24h;
    /** account_feature_snapshot.avg_transaction_amount_24h */
    private final Double avgTransactionAmount24h;
    /** account_feature_snapshot.deposit_count_24h */
    private final Integer depositCount24h;
    /** account_feature_snapshot.withdraw_count_24h */
    private final Integer withdrawCount24h;
    /** account_feature_snapshot.deposit_amount_24h */
    private final Double depositAmount24h;
    /** account_feature_snapshot.withdraw_amount_24h */
    private final Double withdrawAmount24h;
    /** account_feature_snapshot.deposit_withdraw_ratio_24h */
    private final Double depositWithdrawRatio24h;
    /** account_feature_snapshot.unique_counterparty_count_24h */
    private final Integer uniqueCounterpartyCount24h;
    /** account_feature_snapshot.withdraw_after_deposit_delay_avg_24h */
    private final Double withdrawAfterDepositDelayAvg24h;
    /** account_feature_snapshot.rapid_withdraw_after_deposit_flag_24h */
    private final Boolean rapidWithdrawAfterDepositFlag24h;
    /** account_feature_snapshot.reward_transaction_count_30d */
    private final Integer rewardTransactionCount30d;
    /** account_feature_snapshot.reward_withdraw_delay_avg_30d */
    private final Double rewardWithdrawDelayAvg30d;
    /** account_feature_snapshot.unique_device_count_7d */
    private final Integer uniqueDeviceCount7d;
    /** account_feature_snapshot.device_switch_count_24h */
    private final Integer deviceSwitchCount24h;
    /** account_feature_snapshot.shared_device_accounts_7d */
    private final Integer sharedDeviceAccounts7d;
    /** account_feature_snapshot.security_event_count_24h */
    private final Integer securityEventCount24h;
    /** account_feature_snapshot.rapid_profile_change_flag_24h */
    private final Boolean rapidProfileChangeFlag24h;
    /** account_feature_snapshot.security_change_before_withdraw_flag_24h */
    private final Boolean securityChangeBeforeWithdrawFlag24h;
    /** account_feature_snapshot.shared_ip_accounts_7d */
    private final Integer sharedIpAccounts7d;
    /** account_feature_snapshot.shared_bank_accounts_30d */
    private final Integer sharedBankAccounts30d;
    /** account_feature_snapshot.graph_cluster_size_30d */
    private final Integer graphClusterSize30d;
    /** account_feature_snapshot.risk_neighbor_count_30d */
    private final Integer riskNeighborCount30d;

    private AccountFeatureSnapshot(Builder builder) {
        this.accountId = Objects.requireNonNull(builder.accountId, "accountId");
        this.featureVersion = builder.featureVersion;
        this.generatedAt = Objects.requireNonNull(builder.generatedAt, "generatedAt");
        this.accountAgeDays = builder.accountAgeDays;
        this.kycLevelNumeric = builder.kycLevelNumeric;
        this.registrationIpRiskScore = builder.registrationIpRiskScore;
        this.loginCount24h = builder.loginCount24h;
        this.loginFailureCount24h = builder.loginFailureCount24h;
        this.loginFailureRate24h = builder.loginFailureRate24h;
        this.uniqueIpCount24h = builder.uniqueIpCount24h;
        this.highRiskIpLoginCount24h = builder.highRiskIpLoginCount24h;
        this.vpnIpLoginCount24h = builder.vpnIpLoginCount24h;
        this.newDeviceLoginCount7d = builder.newDeviceLoginCount7d;
        this.nightLoginRatio7d = builder.nightLoginRatio7d;
        this.transactionCount24h = builder.transactionCount24h;
        this.totalAmount24h = builder.totalAmount24h;
        this.avgTransactionAmount24h = builder.avgTransactionAmount24h;
        this.depositCount24h = builder.depositCount24h;
        this.withdrawCount24h = builder.withdrawCount24h;
        this.depositAmount24h = builder.depositAmount24h;
        this.withdrawAmount24h = builder.withdrawAmount24h;
        this.depositWithdrawRatio24h = builder.depositWithdrawRatio24h;
        this.uniqueCounterpartyCount24h = builder.uniqueCounterpartyCount24h;
        this.withdrawAfterDepositDelayAvg24h = builder.withdrawAfterDepositDelayAvg24h;
        this.rapidWithdrawAfterDepositFlag24h = builder.rapidWithdrawAfterDepositFlag24h;
        this.rewardTransactionCount30d = builder.rewardTransactionCount30d;
        this.rewardWithdrawDelayAvg30d = builder.rewardWithdrawDelayAvg30d;
        this.uniqueDeviceCount7d = builder.uniqueDeviceCount7d;
        this.deviceSwitchCount24h = builder.deviceSwitchCount24h;
        this.sharedDeviceAccounts7d = builder.sharedDeviceAccounts7d;
        this.securityEventCount24h = builder.securityEventCount24h;
        this.rapidProfileChangeFlag24h = builder.rapidProfileChangeFlag24h;
        this.securityChangeBeforeWithdrawFlag24h = builder.securityChangeBeforeWithdrawFlag24h;
        this.sharedIpAccounts7d = builder.sharedIpAccounts7d;
        this.sharedBankAccounts30d = builder.sharedBankAccounts30d;
        this.graphClusterSize30d = builder.graphClusterSize30d;
        this.riskNeighborCount30d = builder.riskNeighborCount30d;
    }

    public String accountId() { return accountId; }
    public int featureVersion() { return featureVersion; }
    public Instant generatedAt() { return generatedAt; }
    public Integer accountAgeDays() { return accountAgeDays; }
    public Integer kycLevelNumeric() { return kycLevelNumeric; }
    public Double registrationIpRiskScore() { return registrationIpRiskScore; }
    public Integer loginCount24h() { return loginCount24h; }
    public Integer loginFailureCount24h() { return loginFailureCount24h; }
    public Double loginFailureRate24h() { return loginFailureRate24h; }
    public Integer uniqueIpCount24h() { return uniqueIpCount24h; }
    public Integer highRiskIpLoginCount24h() { return highRiskIpLoginCount24h; }
    public Integer vpnIpLoginCount24h() { return vpnIpLoginCount24h; }
    public Integer newDeviceLoginCount7d() { return newDeviceLoginCount7d; }
    public Double nightLoginRatio7d() { return nightLoginRatio7d; }
    public Integer transactionCount24h() { return transactionCount24h; }
    public Double totalAmount24h() { return totalAmount24h; }
    public Double avgTransactionAmount24h() { return avgTransactionAmount24h; }
    public Integer depositCount24h() { return depositCount24h; }
    public Integer withdrawCount24h() { return withdrawCount24h; }
    public Double depositAmount24h() { return depositAmount24h; }
    public Double withdrawAmount24h() { return withdrawAmount24h; }
    public Double depositWithdrawRatio24h() { return depositWithdrawRatio24h; }
    public Integer uniqueCounterpartyCount24h() { return uniqueCounterpartyCount24h; }
    public Double withdrawAfterDepositDelayAvg24h() { return withdrawAfterDepositDelayAvg24h; }
    public Boolean rapidWithdrawAfterDepositFlag24h() { return rapidWithdrawAfterDepositFlag24h; }
    public Integer rewardTransactionCount30d() { return rewardTransactionCount30d; }
    public Double rewardWithdrawDelayAvg30d() { return rewardWithdrawDelayAvg30d; }
    public Integer uniqueDeviceCount7d() { return uniqueDeviceCount7d; }
    public Integer deviceSwitchCount24h() { return deviceSwitchCount24h; }
    public Integer sharedDeviceAccounts7d() { return sharedDeviceAccounts7d; }
    public Integer securityEventCount24h() { return securityEventCount24h; }
    public Boolean rapidProfileChangeFlag24h() { return rapidProfileChangeFlag24h; }
    public Boolean securityChangeBeforeWithdrawFlag24h() { return securityChangeBeforeWithdrawFlag24h; }
    public Integer sharedIpAccounts7d() { return sharedIpAccounts7d; }
    public Integer sharedBankAccounts30d() { return sharedBankAccounts30d; }
    public Integer graphClusterSize30d() { return graphClusterSize30d; }
    public Integer riskNeighborCount30d() { return riskNeighborCount30d; }

    public static Builder builder(String accountId, Instant generatedAt) {
        return new Builder(accountId, generatedAt);
    }

    public static final class Builder {
        private final String accountId;
        private final Instant generatedAt;
        private int featureVersion;
        private Integer accountAgeDays;
        private Integer kycLevelNumeric;
        private Double registrationIpRiskScore;
        private Integer loginCount24h;
        private Integer loginFailureCount24h;
        private Double loginFailureRate24h;
        private Integer uniqueIpCount24h;
        private Integer highRiskIpLoginCount24h;
        private Integer vpnIpLoginCount24h;
        private Integer newDeviceLoginCount7d;
        private Double nightLoginRatio7d;
        private Integer transactionCount24h;
        private Double totalAmount24h;
        private Double avgTransactionAmount24h;
        private Integer depositCount24h;
        private Integer withdrawCount24h;
        private Double depositAmount24h;
        private Double withdrawAmount24h;
        private Double depositWithdrawRatio24h;
        private Integer uniqueCounterpartyCount24h;
        private Double withdrawAfterDepositDelayAvg24h;
        private Boolean rapidWithdrawAfterDepositFlag24h;
        private Integer rewardTransactionCount30d;
        private Double rewardWithdrawDelayAvg30d;
        private Integer uniqueDeviceCount7d;
        private Integer deviceSwitchCount24h;
        private Integer sharedDeviceAccounts7d;
        private Integer securityEventCount24h;
        private Boolean rapidProfileChangeFlag24h;
        private Boolean securityChangeBeforeWithdrawFlag24h;
        private Integer sharedIpAccounts7d;
        private Integer sharedBankAccounts30d;
        private Integer graphClusterSize30d;
        private Integer riskNeighborCount30d;

        private Builder(String accountId, Instant generatedAt) {
            this.accountId = accountId;
            this.generatedAt = generatedAt;
        }

        public Builder featureVersion(int value) { this.featureVersion = value; return this; }
        public Builder accountAgeDays(Integer value) { this.accountAgeDays = value; return this; }
        public Builder kycLevelNumeric(Integer value) { this.kycLevelNumeric = value; return this; }
        public Builder registrationIpRiskScore(Double value) { this.registrationIpRiskScore = value; return this; }
        public Builder loginCount24h(Integer value) { this.loginCount24h = value; return this; }
        public Builder loginFailureCount24h(Integer value) { this.loginFailureCount24h = value; return this; }
        public Builder loginFailureRate24h(Double value) { this.loginFailureRate24h = value; return this; }
        public Builder uniqueIpCount24h(Integer value) { this.uniqueIpCount24h = value; return this; }
        public Builder highRiskIpLoginCount24h(Integer value) { this.highRiskIpLoginCount24h = value; return this; }
        public Builder vpnIpLoginCount24h(Integer value) { this.vpnIpLoginCount24h = value; return this; }
        public Builder newDeviceLoginCount7d(Integer value) { this.newDeviceLoginCount7d = value; return this; }
        public Builder nightLoginRatio7d(Double value) { this.nightLoginRatio7d = value; return this; }
        public Builder transactionCount24h(Integer value) { this.transactionCount24h = value; return this; }
        public Builder totalAmount24h(Double value) { this.totalAmount24h = value; return this; }
        public Builder avgTransactionAmount24h(Double value) { this.avgTransactionAmount24h = value; return this; }
        public Builder depositCount24h(Integer value) { this.depositCount24h = value; return this; }
        public Builder withdrawCount24h(Integer value) { this.withdrawCount24h = value; return this; }
        public Builder depositAmount24h(Double value) { this.depositAmount24h = value; return this; }
        public Builder withdrawAmount24h(Double value) { this.withdrawAmount24h = value; return this; }
        public Builder depositWithdrawRatio24h(Double value) { this.depositWithdrawRatio24h = value; return this; }
        public Builder uniqueCounterpartyCount24h(Integer value) { this.uniqueCounterpartyCount24h = value; return this; }
        public Builder withdrawAfterDepositDelayAvg24h(Double value) { this.withdrawAfterDepositDelayAvg24h = value; return this; }
        public Builder rapidWithdrawAfterDepositFlag24h(Boolean value) { this.rapidWithdrawAfterDepositFlag24h = value; return this; }
        public Builder rewardTransactionCount30d(Integer value) { this.rewardTransactionCount30d = value; return this; }
        public Builder rewardWithdrawDelayAvg30d(Double value) { this.rewardWithdrawDelayAvg30d = value; return this; }
        public Builder uniqueDeviceCount7d(Integer value) { this.uniqueDeviceCount7d = value; return this; }
        public Builder deviceSwitchCount24h(Integer value) { this.deviceSwitchCount24h = value; return this; }
        public Builder sharedDeviceAccounts7d(Integer value) { this.sharedDeviceAccounts7d = value; return this; }
        public Builder securityEventCount24h(Integer value) { this.securityEventCount24h = value; return this; }
        public Builder rapidProfileChangeFlag24h(Boolean value) { this.rapidProfileChangeFlag24h = value; return this; }
        public Builder securityChangeBeforeWithdrawFlag24h(Boolean value) { this.securityChangeBeforeWithdrawFlag24h = value; return this; }
        public Builder sharedIpAccounts7d(Integer value) { this.sharedIpAccounts7d = value; return this; }
        public Builder sharedBankAccounts30d(Integer value) { this.sharedBankAccounts30d = value; return this; }
        public Builder graphClusterSize30d(Integer value) { this.graphClusterSize30d = value; return this; }
        public Builder riskNeighborCount30d(Integer value) { this.riskNeighborCount30d = value; return this; }
        public AccountFeatureSnapshot build() { return new AccountFeatureSnapshot(this); }
    }
}
