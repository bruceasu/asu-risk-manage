package me.asu.ta.feature.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureHistory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountFeatureHistoryRepository {
    private static final RowMapper<AccountFeatureHistory> ROW_MAPPER = new AccountFeatureHistoryRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountFeatureHistoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(AccountFeatureHistory history) {
        String sql = """
                insert into account_feature_history (
                    snapshot_time, account_id, feature_version, account_age_days, kyc_level_numeric,
                    registration_ip_risk_score, login_count_24h, login_failure_count_24h, login_failure_rate_24h,
                    unique_ip_count_24h, high_risk_ip_login_count_24h, vpn_ip_login_count_24h, new_device_login_count_7d,
                    night_login_ratio_7d, transaction_count_24h, total_amount_24h, avg_transaction_amount_24h,
                    deposit_count_24h, withdraw_count_24h, deposit_amount_24h, withdraw_amount_24h,
                    deposit_withdraw_ratio_24h, unique_counterparty_count_24h, withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h, reward_transaction_count_30d, reward_withdraw_delay_avg_30d,
                    unique_device_count_7d, device_switch_count_24h, shared_device_accounts_7d, security_event_count_24h,
                    rapid_profile_change_flag_24h, security_change_before_withdraw_flag_24h, shared_ip_accounts_7d,
                    shared_bank_accounts_30d, graph_cluster_size_30d, risk_neighbor_count_30d, anomaly_score_last
                ) values (
                    :snapshotTime, :accountId, :featureVersion, :accountAgeDays, :kycLevelNumeric,
                    :registrationIpRiskScore, :loginCount24h, :loginFailureCount24h, :loginFailureRate24h,
                    :uniqueIpCount24h, :highRiskIpLoginCount24h, :vpnIpLoginCount24h, :newDeviceLoginCount7d,
                    :nightLoginRatio7d, :transactionCount24h, :totalAmount24h, :avgTransactionAmount24h,
                    :depositCount24h, :withdrawCount24h, :depositAmount24h, :withdrawAmount24h,
                    :depositWithdrawRatio24h, :uniqueCounterpartyCount24h, :withdrawAfterDepositDelayAvg24h,
                    :rapidWithdrawAfterDepositFlag24h, :rewardTransactionCount30d, :rewardWithdrawDelayAvg30d,
                    :uniqueDeviceCount7d, :deviceSwitchCount24h, :sharedDeviceAccounts7d, :securityEventCount24h,
                    :rapidProfileChangeFlag24h, :securityChangeBeforeWithdrawFlag24h, :sharedIpAccounts7d,
                    :sharedBankAccounts30d, :graphClusterSize30d, :riskNeighborCount30d, :anomalyScoreLast
                )
                """;
        return jdbcTemplate.update(sql, params(history));
    }

    public int update(AccountFeatureHistory history) {
        String sql = """
                update account_feature_history
                   set snapshot_time = :snapshotTime,
                       account_id = :accountId,
                       feature_version = :featureVersion,
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
                 where snapshot_id = :snapshotId
                """;
        return jdbcTemplate.update(sql, params(history));
    }

    public Optional<AccountFeatureHistory> findById(long snapshotId) {
        String sql = "select * from account_feature_history where snapshot_id = :snapshotId";
        List<AccountFeatureHistory> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("snapshotId", snapshotId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<AccountFeatureHistory> findBatch(List<Long> snapshotIds) {
        String sql = "select * from account_feature_history where snapshot_id in (:snapshotIds) order by snapshot_id";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("snapshotIds", snapshotIds), ROW_MAPPER);
    }

    public Optional<AccountFeatureHistory> findLatestByAccountId(String accountId) {
        String sql = """
                select * from account_feature_history
                 where account_id = :accountId
                 order by snapshot_time desc, snapshot_id desc
                 limit 1
                """;
        List<AccountFeatureHistory> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("accountId", accountId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int insertBatch(List<AccountFeatureHistory> histories) {
        String sql = """
                insert into account_feature_history (
                    snapshot_time, account_id, feature_version, account_age_days, kyc_level_numeric,
                    registration_ip_risk_score, login_count_24h, login_failure_count_24h, login_failure_rate_24h,
                    unique_ip_count_24h, high_risk_ip_login_count_24h, vpn_ip_login_count_24h, new_device_login_count_7d,
                    night_login_ratio_7d, transaction_count_24h, total_amount_24h, avg_transaction_amount_24h,
                    deposit_count_24h, withdraw_count_24h, deposit_amount_24h, withdraw_amount_24h,
                    deposit_withdraw_ratio_24h, unique_counterparty_count_24h, withdraw_after_deposit_delay_avg_24h,
                    rapid_withdraw_after_deposit_flag_24h, reward_transaction_count_30d, reward_withdraw_delay_avg_30d,
                    unique_device_count_7d, device_switch_count_24h, shared_device_accounts_7d, security_event_count_24h,
                    rapid_profile_change_flag_24h, security_change_before_withdraw_flag_24h, shared_ip_accounts_7d,
                    shared_bank_accounts_30d, graph_cluster_size_30d, risk_neighbor_count_30d, anomaly_score_last
                ) values (
                    :snapshotTime, :accountId, :featureVersion, :accountAgeDays, :kycLevelNumeric,
                    :registrationIpRiskScore, :loginCount24h, :loginFailureCount24h, :loginFailureRate24h,
                    :uniqueIpCount24h, :highRiskIpLoginCount24h, :vpnIpLoginCount24h, :newDeviceLoginCount7d,
                    :nightLoginRatio7d, :transactionCount24h, :totalAmount24h, :avgTransactionAmount24h,
                    :depositCount24h, :withdrawCount24h, :depositAmount24h, :withdrawAmount24h,
                    :depositWithdrawRatio24h, :uniqueCounterpartyCount24h, :withdrawAfterDepositDelayAvg24h,
                    :rapidWithdrawAfterDepositFlag24h, :rewardTransactionCount30d, :rewardWithdrawDelayAvg30d,
                    :uniqueDeviceCount7d, :deviceSwitchCount24h, :sharedDeviceAccounts7d, :securityEventCount24h,
                    :rapidProfileChangeFlag24h, :securityChangeBeforeWithdrawFlag24h, :sharedIpAccounts7d,
                    :sharedBankAccounts30d, :graphClusterSize30d, :riskNeighborCount30d, :anomalyScoreLast
                )
                """;
        return jdbcTemplate.batchUpdate(sql, histories.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(AccountFeatureHistory history) {
        return new MapSqlParameterSource()
                .addValue("snapshotId", history.snapshotId())
                .addValue("snapshotTime", Timestamp.from(history.snapshotTime()))
                .addValue("accountId", history.accountId())
                .addValue("featureVersion", history.featureVersion())
                .addValue("accountAgeDays", history.accountAgeDays())
                .addValue("kycLevelNumeric", history.kycLevelNumeric())
                .addValue("registrationIpRiskScore", history.registrationIpRiskScore())
                .addValue("loginCount24h", history.loginCount24h())
                .addValue("loginFailureCount24h", history.loginFailureCount24h())
                .addValue("loginFailureRate24h", history.loginFailureRate24h())
                .addValue("uniqueIpCount24h", history.uniqueIpCount24h())
                .addValue("highRiskIpLoginCount24h", history.highRiskIpLoginCount24h())
                .addValue("vpnIpLoginCount24h", history.vpnIpLoginCount24h())
                .addValue("newDeviceLoginCount7d", history.newDeviceLoginCount7d())
                .addValue("nightLoginRatio7d", history.nightLoginRatio7d())
                .addValue("transactionCount24h", history.transactionCount24h())
                .addValue("totalAmount24h", history.totalAmount24h())
                .addValue("avgTransactionAmount24h", history.avgTransactionAmount24h())
                .addValue("depositCount24h", history.depositCount24h())
                .addValue("withdrawCount24h", history.withdrawCount24h())
                .addValue("depositAmount24h", history.depositAmount24h())
                .addValue("withdrawAmount24h", history.withdrawAmount24h())
                .addValue("depositWithdrawRatio24h", history.depositWithdrawRatio24h())
                .addValue("uniqueCounterpartyCount24h", history.uniqueCounterpartyCount24h())
                .addValue("withdrawAfterDepositDelayAvg24h", history.withdrawAfterDepositDelayAvg24h())
                .addValue("rapidWithdrawAfterDepositFlag24h", history.rapidWithdrawAfterDepositFlag24h())
                .addValue("rewardTransactionCount30d", history.rewardTransactionCount30d())
                .addValue("rewardWithdrawDelayAvg30d", history.rewardWithdrawDelayAvg30d())
                .addValue("uniqueDeviceCount7d", history.uniqueDeviceCount7d())
                .addValue("deviceSwitchCount24h", history.deviceSwitchCount24h())
                .addValue("sharedDeviceAccounts7d", history.sharedDeviceAccounts7d())
                .addValue("securityEventCount24h", history.securityEventCount24h())
                .addValue("rapidProfileChangeFlag24h", history.rapidProfileChangeFlag24h())
                .addValue("securityChangeBeforeWithdrawFlag24h", history.securityChangeBeforeWithdrawFlag24h())
                .addValue("sharedIpAccounts7d", history.sharedIpAccounts7d())
                .addValue("sharedBankAccounts30d", history.sharedBankAccounts30d())
                .addValue("graphClusterSize30d", history.graphClusterSize30d())
                .addValue("riskNeighborCount30d", history.riskNeighborCount30d())
                .addValue("anomalyScoreLast", history.anomalyScoreLast());
    }

    private static final class AccountFeatureHistoryRowMapper implements RowMapper<AccountFeatureHistory> {
        @Override
        public AccountFeatureHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AccountFeatureHistory(
                    rs.getLong("snapshot_id"),
                    toInstant(rs.getTimestamp("snapshot_time")),
                    rs.getString("account_id"),
                    rs.getInt("feature_version"),
                    (Integer) rs.getObject("account_age_days"),
                    (Integer) rs.getObject("kyc_level_numeric"),
                    (Double) rs.getObject("registration_ip_risk_score"),
                    (Integer) rs.getObject("login_count_24h"),
                    (Integer) rs.getObject("login_failure_count_24h"),
                    (Double) rs.getObject("login_failure_rate_24h"),
                    (Integer) rs.getObject("unique_ip_count_24h"),
                    (Integer) rs.getObject("high_risk_ip_login_count_24h"),
                    (Integer) rs.getObject("vpn_ip_login_count_24h"),
                    (Integer) rs.getObject("new_device_login_count_7d"),
                    (Double) rs.getObject("night_login_ratio_7d"),
                    (Integer) rs.getObject("transaction_count_24h"),
                    (Double) rs.getObject("total_amount_24h"),
                    (Double) rs.getObject("avg_transaction_amount_24h"),
                    (Integer) rs.getObject("deposit_count_24h"),
                    (Integer) rs.getObject("withdraw_count_24h"),
                    (Double) rs.getObject("deposit_amount_24h"),
                    (Double) rs.getObject("withdraw_amount_24h"),
                    (Double) rs.getObject("deposit_withdraw_ratio_24h"),
                    (Integer) rs.getObject("unique_counterparty_count_24h"),
                    (Double) rs.getObject("withdraw_after_deposit_delay_avg_24h"),
                    (Boolean) rs.getObject("rapid_withdraw_after_deposit_flag_24h"),
                    (Integer) rs.getObject("reward_transaction_count_30d"),
                    (Double) rs.getObject("reward_withdraw_delay_avg_30d"),
                    (Integer) rs.getObject("unique_device_count_7d"),
                    (Integer) rs.getObject("device_switch_count_24h"),
                    (Integer) rs.getObject("shared_device_accounts_7d"),
                    (Integer) rs.getObject("security_event_count_24h"),
                    (Boolean) rs.getObject("rapid_profile_change_flag_24h"),
                    (Boolean) rs.getObject("security_change_before_withdraw_flag_24h"),
                    (Integer) rs.getObject("shared_ip_accounts_7d"),
                    (Integer) rs.getObject("shared_bank_accounts_30d"),
                    (Integer) rs.getObject("graph_cluster_size_30d"),
                    (Integer) rs.getObject("risk_neighbor_count_30d"),
                    (Double) rs.getObject("anomaly_score_last"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
