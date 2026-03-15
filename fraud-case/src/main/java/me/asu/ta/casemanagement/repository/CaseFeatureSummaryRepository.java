package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseFeatureSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseFeatureSummaryRepository {
    private static final RowMapper<CaseFeatureSummary> ROW_MAPPER = new CaseFeatureSummaryRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseFeatureSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(CaseFeatureSummary summary) {
        return jdbcTemplate.update("""
                insert into case_feature_summary(
                    case_id, account_age_days, high_risk_ip_login_count_24h, login_failure_rate_24h,
                    new_device_login_count_7d, withdraw_after_deposit_delay_avg_24h, shared_device_accounts_7d,
                    security_change_before_withdraw_flag_24h, graph_cluster_size_30d, risk_neighbor_count_30d,
                    anomaly_score_last, created_at
                ) values (
                    :caseId, :accountAgeDays, :highRiskIpLoginCount24h, :loginFailureRate24h,
                    :newDeviceLoginCount7d, :withdrawAfterDepositDelayAvg24h, :sharedDeviceAccounts7d,
                    :securityChangeBeforeWithdrawFlag24h, :graphClusterSize30d, :riskNeighborCount30d,
                    :anomalyScoreLast, :createdAt
                )
                """, params(summary));
    }

    public Optional<CaseFeatureSummary> findByCaseId(long caseId) {
        List<CaseFeatureSummary> rows = jdbcTemplate.query(
                "select * from case_feature_summary where case_id = :caseId",
                new MapSqlParameterSource("caseId", caseId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(CaseFeatureSummary summary) {
        return new MapSqlParameterSource()
                .addValue("caseId", summary.caseId())
                .addValue("accountAgeDays", summary.accountAgeDays())
                .addValue("highRiskIpLoginCount24h", summary.highRiskIpLoginCount24h())
                .addValue("loginFailureRate24h", summary.loginFailureRate24h())
                .addValue("newDeviceLoginCount7d", summary.newDeviceLoginCount7d())
                .addValue("withdrawAfterDepositDelayAvg24h", summary.withdrawAfterDepositDelayAvg24h())
                .addValue("sharedDeviceAccounts7d", summary.sharedDeviceAccounts7d())
                .addValue("securityChangeBeforeWithdrawFlag24h", summary.securityChangeBeforeWithdrawFlag24h())
                .addValue("graphClusterSize30d", summary.graphClusterSize30d())
                .addValue("riskNeighborCount30d", summary.riskNeighborCount30d())
                .addValue("anomalyScoreLast", summary.anomalyScoreLast())
                .addValue("createdAt", Timestamp.from(summary.createdAt()));
    }

    private static final class CaseFeatureSummaryRowMapper implements RowMapper<CaseFeatureSummary> {
        @Override
        public CaseFeatureSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseFeatureSummary(
                    rs.getLong("case_id"),
                    intOrNull(rs, "account_age_days"),
                    intOrNull(rs, "high_risk_ip_login_count_24h"),
                    doubleOrNull(rs, "login_failure_rate_24h"),
                    intOrNull(rs, "new_device_login_count_7d"),
                    doubleOrNull(rs, "withdraw_after_deposit_delay_avg_24h"),
                    intOrNull(rs, "shared_device_accounts_7d"),
                    boolOrNull(rs, "security_change_before_withdraw_flag_24h"),
                    intOrNull(rs, "graph_cluster_size_30d"),
                    intOrNull(rs, "risk_neighbor_count_30d"),
                    doubleOrNull(rs, "anomaly_score_last"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }

        private static Boolean boolOrNull(ResultSet rs, String column) throws SQLException {
            boolean value = rs.getBoolean(column);
            return rs.wasNull() ? null : value;
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
