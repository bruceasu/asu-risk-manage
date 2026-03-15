package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.rule.model.RuleSeverity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseRuleHitRepository {
    private static final RowMapper<CaseRuleHit> ROW_MAPPER = new CaseRuleHitRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseRuleHitRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertBatch(List<CaseRuleHit> hits) {
        if (hits.isEmpty()) {
            return 0;
        }
        String sql = """
                insert into case_rule_hit(
                    case_id, rule_code, rule_version, severity, score,
                    reason_code, message, evidence_json, created_at
                ) values (
                    :caseId, :ruleCode, :ruleVersion, :severity, :score,
                    :reasonCode, :message, :evidenceJson, :createdAt
                )
                """;
        return jdbcTemplate.batchUpdate(sql, hits.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    public List<CaseRuleHit> findByCaseId(long caseId) {
        return jdbcTemplate.query("""
                select * from case_rule_hit
                 where case_id = :caseId
                 order by score desc, case_rule_hit_id asc
                """, new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    private MapSqlParameterSource params(CaseRuleHit hit) {
        return new MapSqlParameterSource()
                .addValue("caseId", hit.caseId())
                .addValue("ruleCode", hit.ruleCode())
                .addValue("ruleVersion", hit.ruleVersion())
                .addValue("severity", hit.severity().name())
                .addValue("score", hit.score())
                .addValue("reasonCode", hit.reasonCode())
                .addValue("message", hit.message())
                .addValue("evidenceJson", hit.evidenceJson())
                .addValue("createdAt", Timestamp.from(hit.createdAt()));
    }

    private static final class CaseRuleHitRowMapper implements RowMapper<CaseRuleHit> {
        @Override
        public CaseRuleHit mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseRuleHit(
                    rs.getLong("case_rule_hit_id"),
                    rs.getLong("case_id"),
                    rs.getString("rule_code"),
                    rs.getInt("rule_version"),
                    RuleSeverity.valueOf(rs.getString("severity")),
                    rs.getInt("score"),
                    rs.getString("reason_code"),
                    rs.getString("message"),
                    rs.getString("evidence_json"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
