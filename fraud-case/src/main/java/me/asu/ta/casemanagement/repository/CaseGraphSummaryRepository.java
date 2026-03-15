package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseGraphSummaryRepository {
    private static final RowMapper<CaseGraphSummary> ROW_MAPPER = new CaseGraphSummaryRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseGraphSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(CaseGraphSummary summary) {
        return jdbcTemplate.update("""
                insert into case_graph_summary(
                    case_id, graph_score, graph_cluster_size, risk_neighbor_count,
                    shared_device_accounts, shared_bank_accounts, created_at
                ) values (
                    :caseId, :graphScore, :graphClusterSize, :riskNeighborCount,
                    :sharedDeviceAccounts, :sharedBankAccounts, :createdAt
                )
                """, params(summary));
    }

    public Optional<CaseGraphSummary> findByCaseId(long caseId) {
        List<CaseGraphSummary> rows = jdbcTemplate.query(
                "select * from case_graph_summary where case_id = :caseId",
                new MapSqlParameterSource("caseId", caseId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(CaseGraphSummary summary) {
        return new MapSqlParameterSource()
                .addValue("caseId", summary.caseId())
                .addValue("graphScore", summary.graphScore())
                .addValue("graphClusterSize", summary.graphClusterSize())
                .addValue("riskNeighborCount", summary.riskNeighborCount())
                .addValue("sharedDeviceAccounts", summary.sharedDeviceAccounts())
                .addValue("sharedBankAccounts", summary.sharedBankAccounts())
                .addValue("createdAt", Timestamp.from(summary.createdAt()));
    }

    private static final class CaseGraphSummaryRowMapper implements RowMapper<CaseGraphSummary> {
        @Override
        public CaseGraphSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseGraphSummary(
                    rs.getLong("case_id"),
                    doubleOrNull(rs, "graph_score"),
                    intOrNull(rs, "graph_cluster_size"),
                    intOrNull(rs, "risk_neighbor_count"),
                    intOrNull(rs, "shared_device_accounts"),
                    intOrNull(rs, "shared_bank_accounts"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        }

        private static Integer intOrNull(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
