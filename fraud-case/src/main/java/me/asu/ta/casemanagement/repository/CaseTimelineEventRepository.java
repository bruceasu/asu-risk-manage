package me.asu.ta.casemanagement.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CaseTimelineEventRepository {
    private static final RowMapper<CaseTimelineEvent> ROW_MAPPER = new CaseTimelineEventRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CaseTimelineEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertBatch(List<CaseTimelineEvent> events) {
        if (events.isEmpty()) {
            return 0;
        }
        String sql = """
                insert into case_timeline_event(
                    case_id, event_time, event_type, title,
                    description, evidence_json, created_at
                ) values (
                    :caseId, :eventTime, :eventType, :title,
                    :description, :evidenceJson, :createdAt
                )
                """;
        return jdbcTemplate.batchUpdate(sql, events.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    public List<CaseTimelineEvent> findByCaseId(long caseId) {
        return jdbcTemplate.query("""
                select * from case_timeline_event
                 where case_id = :caseId
                 order by event_time asc, timeline_event_id asc
                """, new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    private MapSqlParameterSource params(CaseTimelineEvent event) {
        return new MapSqlParameterSource()
                .addValue("caseId", event.caseId())
                .addValue("eventTime", Timestamp.from(event.eventTime()))
                .addValue("eventType", event.eventType())
                .addValue("title", event.title())
                .addValue("description", event.description())
                .addValue("evidenceJson", event.evidenceJson())
                .addValue("createdAt", Timestamp.from(event.createdAt()));
    }

    private static final class CaseTimelineEventRowMapper implements RowMapper<CaseTimelineEvent> {
        @Override
        public CaseTimelineEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CaseTimelineEvent(
                    rs.getLong("timeline_event_id"),
                    rs.getLong("case_id"),
                    toInstant(rs.getTimestamp("event_time")),
                    rs.getString("event_type"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("evidence_json"),
                    toInstant(rs.getTimestamp("created_at")));
        }

        private static Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
