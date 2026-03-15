package me.asu.ta.rule.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleSeverity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RuleDefinitionRepository {
    private static final RowMapper<RuleDefinition> ROW_MAPPER = new RuleDefinitionRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RuleDefinitionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RuleDefinition definition) {
        String sql = """
                insert into rule_definition(
                    rule_code, rule_name, category, description, severity,
                    owner_module, current_version, is_active, created_at, updated_at
                ) values (
                    :ruleCode, :ruleName, :category, :description, :severity,
                    :ownerModule, :currentVersion, :active, :createdAt, :updatedAt
                )
                on conflict (rule_code) do nothing
                """;
        return jdbcTemplate.update(sql, params(definition));
    }

    public int update(RuleDefinition definition) {
        String sql = """
                update rule_definition
                   set rule_name = :ruleName,
                       category = :category,
                       description = :description,
                       severity = :severity,
                       owner_module = :ownerModule,
                       current_version = :currentVersion,
                       is_active = :active,
                       updated_at = :updatedAt
                 where rule_code = :ruleCode
                """;
        return jdbcTemplate.update(sql, params(definition));
    }

    public Optional<RuleDefinition> findByRuleCode(String ruleCode) {
        String sql = "select * from rule_definition where rule_code = :ruleCode";
        List<RuleDefinition> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("ruleCode", ruleCode), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<RuleDefinition> findActiveRules() {
        return jdbcTemplate.query("select * from rule_definition where is_active = true order by category, rule_code", ROW_MAPPER);
    }

    private MapSqlParameterSource params(RuleDefinition definition) {
        return new MapSqlParameterSource()
                .addValue("ruleCode", definition.ruleCode())
                .addValue("ruleName", definition.ruleName())
                .addValue("category", definition.category().name())
                .addValue("description", definition.description())
                .addValue("severity", definition.severity().name())
                .addValue("ownerModule", definition.ownerModule())
                .addValue("currentVersion", definition.currentVersion())
                .addValue("active", definition.active())
                .addValue("createdAt", Timestamp.from(definition.createdAt()))
                .addValue("updatedAt", Timestamp.from(definition.updatedAt()));
    }

    private static final class RuleDefinitionRowMapper implements RowMapper<RuleDefinition> {
        @Override
        public RuleDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RuleDefinition(
                    rs.getString("rule_code"),
                    rs.getString("rule_name"),
                    RuleCategory.valueOf(rs.getString("category")),
                    rs.getString("description"),
                    RuleSeverity.valueOf(rs.getString("severity")),
                    rs.getString("owner_module"),
                    rs.getInt("current_version"),
                    rs.getBoolean("is_active"),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
