package me.asu.ta.rule.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.rule.model.RuleVersion;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RuleVersionRepository {
    private static final RowMapper<RuleVersion> ROW_MAPPER = new RuleVersionRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RuleVersionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RuleVersion version) {
        String sql = """
                insert into rule_version(
                    rule_code, version, parameter_json, score_weight, enabled,
                    effective_from, effective_to, created_at, created_by, change_note
                ) values (
                    :ruleCode, :version, :parameterJson, :scoreWeight, :enabled,
                    :effectiveFrom, :effectiveTo, :createdAt, :createdBy, :changeNote
                )
                on conflict (rule_code, version) do nothing
                """;
        return jdbcTemplate.update(sql, params(version));
    }

    public int update(RuleVersion version) {
        String sql = """
                update rule_version
                   set parameter_json = :parameterJson,
                       score_weight = :scoreWeight,
                       enabled = :enabled,
                       effective_from = :effectiveFrom,
                       effective_to = :effectiveTo,
                       created_at = :createdAt,
                       created_by = :createdBy,
                       change_note = :changeNote
                 where rule_code = :ruleCode
                   and version = :version
                """;
        return jdbcTemplate.update(sql, params(version));
    }

    public List<RuleVersion> findByRuleCode(String ruleCode) {
        String sql = "select * from rule_version where rule_code = :ruleCode order by version desc";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("ruleCode", ruleCode), ROW_MAPPER);
    }

    public List<RuleVersion> findEffectiveVersions(Instant asOfTime) {
        String sql = """
                select * from rule_version
                 where enabled = true
                   and effective_from <= :asOfTime
                   and (effective_to is null or effective_to > :asOfTime)
                 order by rule_code, version desc
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("asOfTime", Timestamp.from(asOfTime)), ROW_MAPPER);
    }

    private MapSqlParameterSource params(RuleVersion version) {
        return new MapSqlParameterSource()
                .addValue("ruleCode", version.ruleCode())
                .addValue("version", version.version())
                .addValue("parameterJson", version.parameterJson())
                .addValue("scoreWeight", version.scoreWeight())
                .addValue("enabled", version.enabled())
                .addValue("effectiveFrom", Timestamp.from(version.effectiveFrom()))
                .addValue("effectiveTo", toTimestamp(version.effectiveTo()))
                .addValue("createdAt", Timestamp.from(version.createdAt()))
                .addValue("createdBy", version.createdBy())
                .addValue("changeNote", version.changeNote());
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static final class RuleVersionRowMapper implements RowMapper<RuleVersion> {
        @Override
        public RuleVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RuleVersion(
                    rs.getString("rule_code"),
                    rs.getInt("version"),
                    rs.getString("parameter_json"),
                    rs.getInt("score_weight"),
                    rs.getBoolean("enabled"),
                    toInstant(rs.getTimestamp("effective_from")),
                    toInstant(rs.getTimestamp("effective_to")),
                    toInstant(rs.getTimestamp("created_at")),
                    rs.getString("created_by"),
                    rs.getString("change_note"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
