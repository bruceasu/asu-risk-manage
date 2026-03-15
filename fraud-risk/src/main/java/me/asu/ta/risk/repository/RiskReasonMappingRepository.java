package me.asu.ta.risk.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.risk.model.RiskReasonMapping;
import me.asu.ta.rule.model.RuleSeverity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RiskReasonMappingRepository {
    private static final RowMapper<RiskReasonMapping> ROW_MAPPER = new RiskReasonMappingRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskReasonMappingRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RiskReasonMapping mapping) {
        String sql = """
                insert into risk_reason_mapping(
                    reason_code, reason_title, reason_description, severity,
                    category, created_at, updated_at
                ) values (
                    :reasonCode, :reasonTitle, :reasonDescription, :severity,
                    :category, :createdAt, :updatedAt
                )
                on conflict (reason_code) do update set
                    reason_title = excluded.reason_title,
                    reason_description = excluded.reason_description,
                    severity = excluded.severity,
                    category = excluded.category,
                    updated_at = excluded.updated_at
                """;
        return jdbcTemplate.update(sql, params(mapping));
    }

    public Optional<RiskReasonMapping> findReasonMapping(String reasonCode) {
        String sql = "select * from risk_reason_mapping where reason_code = :reasonCode";
        List<RiskReasonMapping> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("reasonCode", reasonCode), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<RiskReasonMapping> findAllMappings() {
        return jdbcTemplate.query("select * from risk_reason_mapping order by reason_code", ROW_MAPPER);
    }

    private MapSqlParameterSource params(RiskReasonMapping mapping) {
        return new MapSqlParameterSource()
                .addValue("reasonCode", mapping.reasonCode())
                .addValue("reasonTitle", mapping.reasonTitle())
                .addValue("reasonDescription", mapping.reasonDescription())
                .addValue("severity", mapping.severity().name())
                .addValue("category", mapping.category())
                .addValue("createdAt", Timestamp.from(mapping.createdAt()))
                .addValue("updatedAt", Timestamp.from(mapping.updatedAt()));
    }

    private static final class RiskReasonMappingRowMapper implements RowMapper<RiskReasonMapping> {
        @Override
        public RiskReasonMapping mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskReasonMapping(
                    rs.getString("reason_code"),
                    rs.getString("reason_title"),
                    rs.getString("reason_description"),
                    RuleSeverity.valueOf(rs.getString("severity")),
                    rs.getString("category"),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
