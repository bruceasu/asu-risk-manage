package me.asu.ta.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import me.asu.ta.ai.model.AiGenerationRequestLog;
import me.asu.ta.ai.model.AiRequestStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiGenerationRequestLogRepository {
    private static final RowMapper<AiGenerationRequestLog> ROW_MAPPER = new AiGenerationRequestLogRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AiGenerationRequestLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiGenerationRequestLog save(AiGenerationRequestLog requestLog) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into ai_generation_request_log(
                    case_id, template_code, template_version, model_name, request_payload,
                    requested_at, status, error_message
                ) values (
                    :caseId, :templateCode, :templateVersion, :modelName, :requestPayload,
                    :requestedAt, :status, :errorMessage
                )
                """, new MapSqlParameterSource()
                .addValue("caseId", requestLog.caseId())
                .addValue("templateCode", requestLog.templateCode())
                .addValue("templateVersion", requestLog.templateVersion())
                .addValue("modelName", requestLog.modelName())
                .addValue("requestPayload", requestLog.requestPayload())
                .addValue("requestedAt", Timestamp.from(requestLog.requestedAt()))
                .addValue("status", requestLog.status().name())
                .addValue("errorMessage", requestLog.errorMessage()), keyHolder, new String[]{"request_id"});
        Number key = keyHolder.getKey();
        return new AiGenerationRequestLog(
                key == null ? null : key.longValue(),
                requestLog.caseId(),
                requestLog.templateCode(),
                requestLog.templateVersion(),
                requestLog.modelName(),
                requestLog.requestPayload(),
                requestLog.requestedAt(),
                requestLog.status(),
                requestLog.errorMessage());
    }

    public int updateStatus(long requestId, AiRequestStatus status, String errorMessage) {
        return jdbcTemplate.update("""
                update ai_generation_request_log
                   set status = :status,
                       error_message = :errorMessage
                 where request_id = :requestId
                """, new MapSqlParameterSource()
                .addValue("requestId", requestId)
                .addValue("status", status.name())
                .addValue("errorMessage", errorMessage));
    }

    public List<AiGenerationRequestLog> findByCaseId(long caseId) {
        return jdbcTemplate.query("""
                select *
                  from ai_generation_request_log
                 where case_id = :caseId
                 order by requested_at desc, request_id desc
                """, new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    private static final class AiGenerationRequestLogRowMapper implements RowMapper<AiGenerationRequestLog> {
        @Override
        public AiGenerationRequestLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AiGenerationRequestLog(
                    rs.getLong("request_id"),
                    rs.getLong("case_id"),
                    rs.getString("template_code"),
                    rs.getInt("template_version"),
                    rs.getString("model_name"),
                    rs.getString("request_payload"),
                    rs.getTimestamp("requested_at").toInstant(),
                    AiRequestStatus.valueOf(rs.getString("status")),
                    rs.getString("error_message"));
        }
    }
}
