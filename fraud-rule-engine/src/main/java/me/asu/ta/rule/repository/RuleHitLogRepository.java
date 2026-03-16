package me.asu.ta.rule.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleHitLog;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RuleHitLogRepository {
    private static final RowMapper<RuleHitLog> ROW_MAPPER = new RuleHitLogRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RuleHitLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RuleHitLog hitLog) {
        return insertRuleHit(hitLog);
    }

    public int insertRuleHit(RuleHitLog hitLog) {
        String sql = """
                insert into rule_hit_log(
                    account_id, rule_code, rule_version, hit_time, score,
                    reason_code, evidence_json, feature_version, evaluation_mode
                ) values (
                    :accountId, :ruleCode, :ruleVersion, :hitTime, :score,
                    :reasonCode, :evidenceJson, :featureVersion, :evaluationMode
                )
                """;
        return jdbcTemplate.update(sql, params(hitLog));
    }

    public List<RuleHitLog> findByRuleCode(String ruleCode) {
        String sql = "select * from rule_hit_log where rule_code = :ruleCode order by hit_time desc";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("ruleCode", ruleCode), ROW_MAPPER);
    }

    public List<RuleHitLog> findLatestByAccountId(String accountId, int limit) {
        String sql = """
                select * from rule_hit_log
                 where account_id = :accountId
                 order by hit_time desc, hit_id desc
                 limit :limit
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("accountId", accountId)
                        .addValue("limit", limit),
                ROW_MAPPER);
    }

    private MapSqlParameterSource params(RuleHitLog hitLog) {
        return new MapSqlParameterSource()
                .addValue("accountId", hitLog.accountId())
                .addValue("ruleCode", hitLog.ruleCode())
                .addValue("ruleVersion", hitLog.ruleVersion())
                .addValue("hitTime", Timestamp.from(hitLog.hitTime()))
                .addValue("score", hitLog.score())
                .addValue("reasonCode", hitLog.reasonCode())
                .addValue("evidenceJson", hitLog.evidenceJson())
                .addValue("featureVersion", hitLog.featureVersion())
                .addValue("evaluationMode", hitLog.evaluationMode().name());
    }

    private static final class RuleHitLogRowMapper implements RowMapper<RuleHitLog> {
        @Override
        public RuleHitLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RuleHitLog(
                    rs.getLong("hit_id"),
                    rs.getString("account_id"),
                    rs.getString("rule_code"),
                    rs.getInt("rule_version"),
                    toInstant(rs.getTimestamp("hit_time")),
                    rs.getInt("score"),
                    rs.getString("reason_code"),
                    rs.getString("evidence_json"),
                    rs.getInt("feature_version"),
                    EvaluationMode.valueOf(rs.getString("evaluation_mode")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
