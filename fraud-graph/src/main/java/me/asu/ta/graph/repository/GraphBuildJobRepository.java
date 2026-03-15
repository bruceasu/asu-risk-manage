package me.asu.ta.graph.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.graph.model.GraphBuildJob;
import me.asu.ta.graph.model.GraphBuildJobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class GraphBuildJobRepository {
    private static final RowMapper<GraphBuildJob> ROW_MAPPER = new GraphBuildJobRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GraphBuildJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GraphBuildJob save(GraphBuildJob job) {
        return createJob(job);
    }

    public GraphBuildJob createJob(GraphBuildJob job) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into graph_build_job(
                    job_type, graph_window_start, graph_window_end, started_at, finished_at, status,
                    processed_account_count, generated_edge_count, generated_cluster_count, error_message
                ) values (
                    :jobType, :graphWindowStart, :graphWindowEnd, :startedAt, :finishedAt, :status,
                    :processedAccountCount, :generatedEdgeCount, :generatedClusterCount, :errorMessage
                )
                """, params(job), keyHolder, new String[]{"job_id"});
        Number key = keyHolder.getKey();
        return new GraphBuildJob(
                key == null ? 0L : key.longValue(),
                job.jobType(),
                job.graphWindowStart(),
                job.graphWindowEnd(),
                job.startedAt(),
                job.finishedAt(),
                job.status(),
                job.processedAccountCount(),
                job.generatedEdgeCount(),
                job.generatedClusterCount(),
                job.errorMessage());
    }

    public int updateJobStatus(GraphBuildJob job) {
        return jdbcTemplate.update("""
                update graph_build_job
                   set finished_at = :finishedAt,
                       status = :status,
                       processed_account_count = :processedAccountCount,
                       generated_edge_count = :generatedEdgeCount,
                       generated_cluster_count = :generatedClusterCount,
                       error_message = :errorMessage
                 where job_id = :jobId
                """, params(job).addValue("jobId", job.jobId()));
    }

    public Optional<GraphBuildJob> findById(long jobId) {
        List<GraphBuildJob> rows = jdbcTemplate.query(
                "select * from graph_build_job where job_id = :jobId",
                new MapSqlParameterSource("jobId", jobId),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource params(GraphBuildJob job) {
        return new MapSqlParameterSource()
                .addValue("jobType", job.jobType())
                .addValue("graphWindowStart", Timestamp.from(job.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(job.graphWindowEnd()))
                .addValue("startedAt", Timestamp.from(job.startedAt()))
                .addValue("finishedAt", toTimestamp(job.finishedAt()))
                .addValue("status", job.status().name())
                .addValue("processedAccountCount", job.processedAccountCount())
                .addValue("generatedEdgeCount", job.generatedEdgeCount())
                .addValue("generatedClusterCount", job.generatedClusterCount())
                .addValue("errorMessage", job.errorMessage());
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static final class GraphBuildJobRowMapper implements RowMapper<GraphBuildJob> {
        @Override
        public GraphBuildJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GraphBuildJob(
                    rs.getLong("job_id"),
                    rs.getString("job_type"),
                    rs.getTimestamp("graph_window_start").toInstant(),
                    rs.getTimestamp("graph_window_end").toInstant(),
                    rs.getTimestamp("started_at").toInstant(),
                    toInstant(rs.getTimestamp("finished_at")),
                    GraphBuildJobStatus.valueOf(rs.getString("status")),
                    intOrNull(rs, "processed_account_count"),
                    intOrNull(rs, "generated_edge_count"),
                    intOrNull(rs, "generated_cluster_count"),
                    rs.getString("error_message"));
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
