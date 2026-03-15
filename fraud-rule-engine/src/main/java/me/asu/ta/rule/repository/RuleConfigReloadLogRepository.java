package me.asu.ta.rule.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.rule.model.RuleConfigReloadLog;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RuleConfigReloadLogRepository {
    private static final RowMapper<RuleConfigReloadLog> ROW_MAPPER = new RuleConfigReloadLogRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RuleConfigReloadLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RuleConfigReloadLog reloadLog) {
        String sql = """
                insert into rule_config_reload_log(
                    reload_time, status, loaded_rule_count, error_message
                ) values (
                    :reloadTime, :status, :loadedRuleCount, :errorMessage
                )
                """;
        return jdbcTemplate.update(sql, params(reloadLog));
    }

    public int update(RuleConfigReloadLog reloadLog) {
        return save(reloadLog);
    }

    public Optional<RuleConfigReloadLog> findByRuleCode(String status) {
        String sql = """
                select * from rule_config_reload_log
                 where status = :status
                 order by reload_time desc, reload_id desc
                 limit 1
                """;
        List<RuleConfigReloadLog> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("status", status), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<RuleConfigReloadLog> findActiveRules() {
        return jdbcTemplate.query("select * from rule_config_reload_log order by reload_time desc", ROW_MAPPER);
    }

    private MapSqlParameterSource params(RuleConfigReloadLog reloadLog) {
        return new MapSqlParameterSource()
                .addValue("reloadTime", Timestamp.from(reloadLog.reloadTime()))
                .addValue("status", reloadLog.status())
                .addValue("loadedRuleCount", reloadLog.loadedRuleCount())
                .addValue("errorMessage", reloadLog.errorMessage());
    }

    private static final class RuleConfigReloadLogRowMapper implements RowMapper<RuleConfigReloadLog> {
        @Override
        public RuleConfigReloadLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RuleConfigReloadLog(
                    rs.getLong("reload_id"),
                    toInstant(rs.getTimestamp("reload_time")),
                    rs.getString("status"),
                    (Integer) rs.getObject("loaded_rule_count"),
                    rs.getString("error_message"));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
