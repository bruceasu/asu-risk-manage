package me.asu.ta.feature.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountFeatureSnapshotRepository {
    private static final RowMapper<AccountFeatureSnapshot> ROW_MAPPER = new SnapshotRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountFeatureSnapshotRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(AccountFeatureSnapshot snapshot) {
        String sql = """
                insert into account_feature_snapshot (
                    account_id, feature_version, generated_at, account_age_days, kyc_level_numeric,
                    registration_ip_risk_score, login_count_24h, login_failure_count_24h, login_failure_rate_24h, unique_ip_count_24h,
                    high_risk_ip_login_count_24h, vpn_ip_login_count_24h, new_device_login_count_7d, night_login_ratio_7d,
                    transaction_count_24h, total_amount_24h, avg_transaction_amount_24h, deposit_count_24h,
                    withdraw_count_24h, deposit_amount_24h, withdraw_amount_24h, deposit_withdraw_ratio_24h,
                    unique_counterparty_count_24h, withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h, reward_transaction_count_30d, reward_withdraw_delay_avg_30d,
                    unique_device_count_7d, device_switch_count_24h, shared_device_accounts_7d, security_event_count_24h,
                    rapid_profile_change_flag_24h, security_change_before_withdraw_flag_24h, shared_ip_accounts_7d,
                    shared_bank_accounts_30d, graph_cluster_size_30d, risk_neighbor_count_30d, anomaly_score_last
                ) values (
                    :accountId, :featureVersion, :generatedAt, :accountAgeDays, :kycLevelNumeric,
                    :registrationIpRiskScore, :loginCount24h, :loginFailureCount24h, :loginFailureRate24h, :uniqueIpCount24h,
                    :highRiskIpLoginCount24h, :vpnIpLoginCount24h, :newDeviceLoginCount7d, :nightLoginRatio7d,
                    :transactionCount24h, :totalAmount24h, :avgTransactionAmount24h, :depositCount24h,
                    :withdrawCount24h, :depositAmount24h, :withdrawAmount24h, :depositWithdrawRatio24h,
                    :uniqueCounterpartyCount24h, :withdrawAfterDepositDelayAvg24h,
                    :rapidWithdrawAfterDepositFlag24h, :rewardTransactionCount30d, :rewardWithdrawDelayAvg30d,
                    :uniqueDeviceCount7d, :deviceSwitchCount24h, :sharedDeviceAccounts7d, :securityEventCount24h,
                    :rapidProfileChangeFlag24h, :securityChangeBeforeWithdrawFlag24h, :sharedIpAccounts7d,
                    :sharedBankAccounts30d, :graphClusterSize30d, :riskNeighborCount30d, :anomalyScoreLast
                )
                on conflict (account_id) do update set
                    feature_version = excluded.feature_version,
                    generated_at = excluded.generated_at,
                    account_age_days = excluded.account_age_days,
                    kyc_level_numeric = excluded.kyc_level_numeric,
                    registration_ip_risk_score = excluded.registration_ip_risk_score,
                    login_count_24h = excluded.login_count_24h,
                    login_failure_count_24h = excluded.login_failure_count_24h,
                    login_failure_rate_24h = excluded.login_failure_rate_24h,
                    unique_ip_count_24h = excluded.unique_ip_count_24h,
                    high_risk_ip_login_count_24h = excluded.high_risk_ip_login_count_24h,
                    vpn_ip_login_count_24h = excluded.vpn_ip_login_count_24h,
                    new_device_login_count_7d = excluded.new_device_login_count_7d,
                    night_login_ratio_7d = excluded.night_login_ratio_7d,
                    transaction_count_24h = excluded.transaction_count_24h,
                    total_amount_24h = excluded.total_amount_24h,
                    avg_transaction_amount_24h = excluded.avg_transaction_amount_24h,
                    deposit_count_24h = excluded.deposit_count_24h,
                    withdraw_count_24h = excluded.withdraw_count_24h,
                    deposit_amount_24h = excluded.deposit_amount_24h,
                    withdraw_amount_24h = excluded.withdraw_amount_24h,
                    deposit_withdraw_ratio_24h = excluded.deposit_withdraw_ratio_24h,
                    unique_counterparty_count_24h = excluded.unique_counterparty_count_24h,
                    withdraw_after_deposit_delay_avg_24h = excluded.withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h = excluded.rapid_withdraw_after_deposit_flag_24h,
                    reward_transaction_count_30d = excluded.reward_transaction_count_30d,
                    reward_withdraw_delay_avg_30d = excluded.reward_withdraw_delay_avg_30d,
                    unique_device_count_7d = excluded.unique_device_count_7d,
                    device_switch_count_24h = excluded.device_switch_count_24h,
                    shared_device_accounts_7d = excluded.shared_device_accounts_7d,
                    security_event_count_24h = excluded.security_event_count_24h,
                    rapid_profile_change_flag_24h = excluded.rapid_profile_change_flag_24h,
                    security_change_before_withdraw_flag_24h = excluded.security_change_before_withdraw_flag_24h,
                    shared_ip_accounts_7d = excluded.shared_ip_accounts_7d,
                    shared_bank_accounts_30d = excluded.shared_bank_accounts_30d,
                    graph_cluster_size_30d = excluded.graph_cluster_size_30d,
                    risk_neighbor_count_30d = excluded.risk_neighbor_count_30d,
                    anomaly_score_last = excluded.anomaly_score_last
                """;
        return jdbcTemplate.update(sql, params(snapshot));
    }

    public int update(AccountFeatureSnapshot snapshot) {
        String sql = """
                update account_feature_snapshot
                   set feature_version = :featureVersion,
                       generated_at = :generatedAt,
                       account_age_days = :accountAgeDays,
                       kyc_level_numeric = :kycLevelNumeric,
                       registration_ip_risk_score = :registrationIpRiskScore,
                       login_count_24h = :loginCount24h,
                       login_failure_count_24h = :loginFailureCount24h,
                       login_failure_rate_24h = :loginFailureRate24h,
                       unique_ip_count_24h = :uniqueIpCount24h,
                       high_risk_ip_login_count_24h = :highRiskIpLoginCount24h,
                       vpn_ip_login_count_24h = :vpnIpLoginCount24h,
                       new_device_login_count_7d = :newDeviceLoginCount7d,
                       night_login_ratio_7d = :nightLoginRatio7d,
                       transaction_count_24h = :transactionCount24h,
                       total_amount_24h = :totalAmount24h,
                       avg_transaction_amount_24h = :avgTransactionAmount24h,
                       deposit_count_24h = :depositCount24h,
                       withdraw_count_24h = :withdrawCount24h,
                       deposit_amount_24h = :depositAmount24h,
                       withdraw_amount_24h = :withdrawAmount24h,
                       deposit_withdraw_ratio_24h = :depositWithdrawRatio24h,
                       unique_counterparty_count_24h = :uniqueCounterpartyCount24h,
                       withdraw_after_deposit_delay_avg_24h = :withdrawAfterDepositDelayAvg24h,
                       rapid_withdraw_after_deposit_flag_24h = :rapidWithdrawAfterDepositFlag24h,
                       reward_transaction_count_30d = :rewardTransactionCount30d,
                       reward_withdraw_delay_avg_30d = :rewardWithdrawDelayAvg30d,
                       unique_device_count_7d = :uniqueDeviceCount7d,
                       device_switch_count_24h = :deviceSwitchCount24h,
                       shared_device_accounts_7d = :sharedDeviceAccounts7d,
                       security_event_count_24h = :securityEventCount24h,
                       rapid_profile_change_flag_24h = :rapidProfileChangeFlag24h,
                       security_change_before_withdraw_flag_24h = :securityChangeBeforeWithdrawFlag24h,
                       shared_ip_accounts_7d = :sharedIpAccounts7d,
                       shared_bank_accounts_30d = :sharedBankAccounts30d,
                       graph_cluster_size_30d = :graphClusterSize30d,
                       risk_neighbor_count_30d = :riskNeighborCount30d,
                       anomaly_score_last = :anomalyScoreLast
                 where account_id = :accountId
                """;
        return jdbcTemplate.update(sql, params(snapshot));
    }

    public Optional<AccountFeatureSnapshot> findById(String accountId) {
        String sql = "select * from account_feature_snapshot where account_id = :accountId";
        List<AccountFeatureSnapshot> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("accountId", accountId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<AccountFeatureSnapshot> findLatestByAccountId(String accountId) {
        return findById(accountId);
    }

    public List<AccountFeatureSnapshot> findBatch(List<String> accountIds) {
        String sql = "select * from account_feature_snapshot where account_id in (:accountIds) order by account_id";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("accountIds", accountIds), ROW_MAPPER);
    }

    public int insertBatch(List<AccountFeatureSnapshot> snapshots) {
        String sql = """
                insert into account_feature_snapshot (
                    account_id, feature_version, generated_at, account_age_days, kyc_level_numeric,
                    registration_ip_risk_score, login_count_24h, login_failure_count_24h, login_failure_rate_24h, unique_ip_count_24h,
                    high_risk_ip_login_count_24h, vpn_ip_login_count_24h, new_device_login_count_7d, night_login_ratio_7d,
                    transaction_count_24h, total_amount_24h, avg_transaction_amount_24h, deposit_count_24h,
                    withdraw_count_24h, deposit_amount_24h, withdraw_amount_24h, deposit_withdraw_ratio_24h,
                    unique_counterparty_count_24h, withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h, reward_transaction_count_30d, reward_withdraw_delay_avg_30d,
                    unique_device_count_7d, device_switch_count_24h, shared_device_accounts_7d, security_event_count_24h,
                    rapid_profile_change_flag_24h, security_change_before_withdraw_flag_24h, shared_ip_accounts_7d,
                    shared_bank_accounts_30d, graph_cluster_size_30d, risk_neighbor_count_30d, anomaly_score_last
                ) values (
                    :accountId, :featureVersion, :generatedAt, :accountAgeDays, :kycLevelNumeric,
                    :registrationIpRiskScore, :loginCount24h, :loginFailureCount24h, :loginFailureRate24h, :uniqueIpCount24h,
                    :highRiskIpLoginCount24h, :vpnIpLoginCount24h, :newDeviceLoginCount7d, :nightLoginRatio7d,
                    :transactionCount24h, :totalAmount24h, :avgTransactionAmount24h, :depositCount24h,
                    :withdrawCount24h, :depositAmount24h, :withdrawAmount24h, :depositWithdrawRatio24h,
                    :uniqueCounterpartyCount24h, :withdrawAfterDepositDelayAvg24h,
                    :rapidWithdrawAfterDepositFlag24h, :rewardTransactionCount30d, :rewardWithdrawDelayAvg30d,
                    :uniqueDeviceCount7d, :deviceSwitchCount24h, :sharedDeviceAccounts7d, :securityEventCount24h,
                    :rapidProfileChangeFlag24h, :securityChangeBeforeWithdrawFlag24h, :sharedIpAccounts7d,
                    :sharedBankAccounts30d, :graphClusterSize30d, :riskNeighborCount30d, :anomalyScoreLast
                )
                on conflict (account_id) do update set
                    feature_version = excluded.feature_version,
                    generated_at = excluded.generated_at,
                    account_age_days = excluded.account_age_days,
                    kyc_level_numeric = excluded.kyc_level_numeric,
                    registration_ip_risk_score = excluded.registration_ip_risk_score,
                    login_count_24h = excluded.login_count_24h,
                    login_failure_count_24h = excluded.login_failure_count_24h,
                    login_failure_rate_24h = excluded.login_failure_rate_24h,
                    unique_ip_count_24h = excluded.unique_ip_count_24h,
                    high_risk_ip_login_count_24h = excluded.high_risk_ip_login_count_24h,
                    vpn_ip_login_count_24h = excluded.vpn_ip_login_count_24h,
                    new_device_login_count_7d = excluded.new_device_login_count_7d,
                    night_login_ratio_7d = excluded.night_login_ratio_7d,
                    transaction_count_24h = excluded.transaction_count_24h,
                    total_amount_24h = excluded.total_amount_24h,
                    avg_transaction_amount_24h = excluded.avg_transaction_amount_24h,
                    deposit_count_24h = excluded.deposit_count_24h,
                    withdraw_count_24h = excluded.withdraw_count_24h,
                    deposit_amount_24h = excluded.deposit_amount_24h,
                    withdraw_amount_24h = excluded.withdraw_amount_24h,
                    deposit_withdraw_ratio_24h = excluded.deposit_withdraw_ratio_24h,
                    unique_counterparty_count_24h = excluded.unique_counterparty_count_24h,
                    withdraw_after_deposit_delay_avg_24h = excluded.withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h = excluded.rapid_withdraw_after_deposit_flag_24h,
                    reward_transaction_count_30d = excluded.reward_transaction_count_30d,
                    reward_withdraw_delay_avg_30d = excluded.reward_withdraw_delay_avg_30d,
                    unique_device_count_7d = excluded.unique_device_count_7d,
                    device_switch_count_24h = excluded.device_switch_count_24h,
                    shared_device_accounts_7d = excluded.shared_device_accounts_7d,
                    security_event_count_24h = excluded.security_event_count_24h,
                    rapid_profile_change_flag_24h = excluded.rapid_profile_change_flag_24h,
                    security_change_before_withdraw_flag_24h = excluded.security_change_before_withdraw_flag_24h,
                    shared_ip_accounts_7d = excluded.shared_ip_accounts_7d,
                    shared_bank_accounts_30d = excluded.shared_bank_accounts_30d,
                    graph_cluster_size_30d = excluded.graph_cluster_size_30d,
                    risk_neighbor_count_30d = excluded.risk_neighbor_count_30d,
                    anomaly_score_last = excluded.anomaly_score_last
                """;
        return jdbcTemplate.batchUpdate(sql, snapshots.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(AccountFeatureSnapshot snapshot) {
        return new MapSqlParameterSource()
                .addValue("accountId", snapshot.accountId())
                .addValue("featureVersion", snapshot.featureVersion())
                .addValue("generatedAt", Timestamp.from(snapshot.generatedAt()))
                .addValue("accountAgeDays", snapshot.accountAgeDays())
                .addValue("kycLevelNumeric", snapshot.kycLevelNumeric())
                .addValue("registrationIpRiskScore", snapshot.registrationIpRiskScore())
                .addValue("loginCount24h", snapshot.loginCount24h())
                .addValue("loginFailureCount24h", snapshot.loginFailureCount24h())
                .addValue("loginFailureRate24h", snapshot.loginFailureRate24h())
                .addValue("uniqueIpCount24h", snapshot.uniqueIpCount24h())
                .addValue("highRiskIpLoginCount24h", snapshot.highRiskIpLoginCount24h())
                .addValue("vpnIpLoginCount24h", snapshot.vpnIpLoginCount24h())
                .addValue("newDeviceLoginCount7d", snapshot.newDeviceLoginCount7d())
                .addValue("nightLoginRatio7d", snapshot.nightLoginRatio7d())
                .addValue("transactionCount24h", snapshot.transactionCount24h())
                .addValue("totalAmount24h", snapshot.totalAmount24h())
                .addValue("avgTransactionAmount24h", snapshot.avgTransactionAmount24h())
                .addValue("depositCount24h", snapshot.depositCount24h())
                .addValue("withdrawCount24h", snapshot.withdrawCount24h())
                .addValue("depositAmount24h", snapshot.depositAmount24h())
                .addValue("withdrawAmount24h", snapshot.withdrawAmount24h())
                .addValue("depositWithdrawRatio24h", snapshot.depositWithdrawRatio24h())
                .addValue("uniqueCounterpartyCount24h", snapshot.uniqueCounterpartyCount24h())
                .addValue("withdrawAfterDepositDelayAvg24h", snapshot.withdrawAfterDepositDelayAvg24h())
                .addValue("rapidWithdrawAfterDepositFlag24h", snapshot.rapidWithdrawAfterDepositFlag24h())
                .addValue("rewardTransactionCount30d", snapshot.rewardTransactionCount30d())
                .addValue("rewardWithdrawDelayAvg30d", snapshot.rewardWithdrawDelayAvg30d())
                .addValue("uniqueDeviceCount7d", snapshot.uniqueDeviceCount7d())
                .addValue("deviceSwitchCount24h", snapshot.deviceSwitchCount24h())
                .addValue("sharedDeviceAccounts7d", snapshot.sharedDeviceAccounts7d())
                .addValue("securityEventCount24h", snapshot.securityEventCount24h())
                .addValue("rapidProfileChangeFlag24h", snapshot.rapidProfileChangeFlag24h())
                .addValue("securityChangeBeforeWithdrawFlag24h", snapshot.securityChangeBeforeWithdrawFlag24h())
                .addValue("sharedIpAccounts7d", snapshot.sharedIpAccounts7d())
                .addValue("sharedBankAccounts30d", snapshot.sharedBankAccounts30d())
                .addValue("graphClusterSize30d", snapshot.graphClusterSize30d())
                .addValue("riskNeighborCount30d", snapshot.riskNeighborCount30d())
                .addValue("anomalyScoreLast", snapshot.anomalyScoreLast());
    }

    private static final class SnapshotRowMapper implements RowMapper<AccountFeatureSnapshot> {
        @Override
        public AccountFeatureSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AccountFeatureSnapshot.builder(rs.getString("account_id"), toInstant(rs.getTimestamp("generated_at")))
                    .featureVersion(rs.getInt("feature_version"))
                    .accountAgeDays((Integer) rs.getObject("account_age_days"))
                    .kycLevelNumeric((Integer) rs.getObject("kyc_level_numeric"))
                    .registrationIpRiskScore((Double) rs.getObject("registration_ip_risk_score"))
                    .loginCount24h((Integer) rs.getObject("login_count_24h"))
                    .loginFailureCount24h((Integer) rs.getObject("login_failure_count_24h"))
                    .loginFailureRate24h((Double) rs.getObject("login_failure_rate_24h"))
                    .uniqueIpCount24h((Integer) rs.getObject("unique_ip_count_24h"))
                    .highRiskIpLoginCount24h((Integer) rs.getObject("high_risk_ip_login_count_24h"))
                    .vpnIpLoginCount24h((Integer) rs.getObject("vpn_ip_login_count_24h"))
                    .newDeviceLoginCount7d((Integer) rs.getObject("new_device_login_count_7d"))
                    .nightLoginRatio7d((Double) rs.getObject("night_login_ratio_7d"))
                    .transactionCount24h((Integer) rs.getObject("transaction_count_24h"))
                    .totalAmount24h((Double) rs.getObject("total_amount_24h"))
                    .avgTransactionAmount24h((Double) rs.getObject("avg_transaction_amount_24h"))
                    .depositCount24h((Integer) rs.getObject("deposit_count_24h"))
                    .withdrawCount24h((Integer) rs.getObject("withdraw_count_24h"))
                    .depositAmount24h((Double) rs.getObject("deposit_amount_24h"))
                    .withdrawAmount24h((Double) rs.getObject("withdraw_amount_24h"))
                    .depositWithdrawRatio24h((Double) rs.getObject("deposit_withdraw_ratio_24h"))
                    .uniqueCounterpartyCount24h((Integer) rs.getObject("unique_counterparty_count_24h"))
                    .withdrawAfterDepositDelayAvg24h((Double) rs.getObject("withdraw_after_deposit_delay_avg_24h"))
                    .rapidWithdrawAfterDepositFlag24h((Boolean) rs.getObject("rapid_withdraw_after_deposit_flag_24h"))
                    .rewardTransactionCount30d((Integer) rs.getObject("reward_transaction_count_30d"))
                    .rewardWithdrawDelayAvg30d((Double) rs.getObject("reward_withdraw_delay_avg_30d"))
                    .uniqueDeviceCount7d((Integer) rs.getObject("unique_device_count_7d"))
                    .deviceSwitchCount24h((Integer) rs.getObject("device_switch_count_24h"))
                    .sharedDeviceAccounts7d((Integer) rs.getObject("shared_device_accounts_7d"))
                    .securityEventCount24h((Integer) rs.getObject("security_event_count_24h"))
                    .rapidProfileChangeFlag24h((Boolean) rs.getObject("rapid_profile_change_flag_24h"))
                    .securityChangeBeforeWithdrawFlag24h((Boolean) rs.getObject("security_change_before_withdraw_flag_24h"))
                    .sharedIpAccounts7d((Integer) rs.getObject("shared_ip_accounts_7d"))
                    .sharedBankAccounts30d((Integer) rs.getObject("shared_bank_accounts_30d"))
                    .graphClusterSize30d((Integer) rs.getObject("graph_cluster_size_30d"))
                    .riskNeighborCount30d((Integer) rs.getObject("risk_neighbor_count_30d"))
                    .anomalyScoreLast((Double) rs.getObject("anomaly_score_last"))
                    .build();
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
