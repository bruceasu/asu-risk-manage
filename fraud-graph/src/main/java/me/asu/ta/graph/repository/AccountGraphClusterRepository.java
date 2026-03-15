package me.asu.ta.graph.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.asu.ta.graph.model.GraphClusterMembership;
import me.asu.ta.graph.model.GraphClusterType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountGraphClusterRepository {
    private static final RowMapper<GraphClusterMembership> ROW_MAPPER = new GraphClusterMembershipRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountGraphClusterRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(GraphClusterMembership membership) {
        return batchInsert(List.of(membership));
    }

    public int batchInsert(List<GraphClusterMembership> memberships) {
        if (memberships.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.batchUpdate("""
                insert into account_graph_cluster(
                    cluster_id, account_id, cluster_type, cluster_size,
                    graph_window_start, graph_window_end, created_at
                ) values (
                    :clusterId, :accountId, :clusterType, :clusterSize,
                    :graphWindowStart, :graphWindowEnd, :createdAt
                )
                """, memberships.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    public int deleteByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.update("""
                delete from account_graph_cluster
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)));
    }

    public List<GraphClusterMembership> findByAccountId(String accountId) {
        return jdbcTemplate.query(
                "select * from account_graph_cluster where account_id = :accountId",
                new MapSqlParameterSource("accountId", accountId),
                ROW_MAPPER);
    }

    public List<GraphClusterMembership> findByClusterId(String clusterId) {
        return jdbcTemplate.query(
                "select * from account_graph_cluster where cluster_id = :clusterId order by account_id asc",
                new MapSqlParameterSource("clusterId", clusterId),
                ROW_MAPPER);
    }

    public List<GraphClusterMembership> findByWindow(Instant graphWindowStart, Instant graphWindowEnd) {
        return jdbcTemplate.query("""
                select * from account_graph_cluster
                 where graph_window_start = :graphWindowStart
                   and graph_window_end = :graphWindowEnd
                """, new MapSqlParameterSource()
                .addValue("graphWindowStart", Timestamp.from(graphWindowStart))
                .addValue("graphWindowEnd", Timestamp.from(graphWindowEnd)), ROW_MAPPER);
    }

    private MapSqlParameterSource params(GraphClusterMembership membership) {
        return new MapSqlParameterSource()
                .addValue("clusterId", membership.clusterId())
                .addValue("accountId", membership.accountId())
                .addValue("clusterType", membership.clusterType().name())
                .addValue("clusterSize", membership.clusterSize())
                .addValue("graphWindowStart", Timestamp.from(membership.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(membership.graphWindowEnd()))
                .addValue("createdAt", Timestamp.from(membership.createdAt()));
    }

    private static final class GraphClusterMembershipRowMapper implements RowMapper<GraphClusterMembership> {
        @Override
        public GraphClusterMembership mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GraphClusterMembership(
                    rs.getString("cluster_id"),
                    rs.getString("account_id"),
                    GraphClusterType.valueOf(rs.getString("cluster_type")),
                    rs.getInt("cluster_size"),
                    rs.getTimestamp("graph_window_start").toInstant(),
                    rs.getTimestamp("graph_window_end").toInstant(),
                    rs.getTimestamp("created_at").toInstant());
        }
    }
}
