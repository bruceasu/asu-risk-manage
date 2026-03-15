package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseRecommendedActionRepository {
    private static final RowMapper<CaseRecommendedAction> ROW_MAPPER = new CaseRecommendedActionRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseRecommendedActionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertBatch(List<CaseRecommendedAction> actions) {
        if (actions.isEmpty()) {
            return 0;
        }
        String sql = """
                insert into case_recommended_action(
                    case_id, action_code, action_reason, created_at
                ) values (
                    :caseId, :actionCode, :actionReason, :createdAt
                )
                """;
        return jdbcTemplate.batchUpdate(sql, actions.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    public List<CaseRecommendedAction> findByCaseId(long caseId) {
        return jdbcTemplate.query("""
                select * from case_recommended_action
                 where case_id = :caseId
                 order by case_action_id asc
                """, new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    private MapSqlParameterSource params(CaseRecommendedAction action) {
        return new MapSqlParameterSource()
                .addValue("caseId", action.caseId())
                .addValue("actionCode", action.actionCode())
                .addValue("actionReason", action.actionReason())
                .addValue("createdAt", Timestamp.from(action.createdAt()));
    }

    private static final class CaseRecommendedActionRowMapper implements RowMapper<CaseRecommendedAction> {
        @Override
        public CaseRecommendedAction mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseRecommendedAction(
                    rs.getLong("case_action_id"),
                    rs.getLong("case_id"),
                    rs.getString("action_code"),
                    rs.getString("action_reason"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
