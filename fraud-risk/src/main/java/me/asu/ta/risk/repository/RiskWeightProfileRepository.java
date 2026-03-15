package me.asu.ta.risk.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.risk.model.RiskWeightProfile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RiskWeightProfileRepository {
    private static final RowMapper<RiskWeightProfile> ROW_MAPPER = new RiskWeightProfileRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskWeightProfileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(RiskWeightProfile profile) {
        String sql = """
                insert into risk_weight_profile(
                    profile_name, rule_weight, graph_weight, anomaly_weight,
                    behavior_weight, enabled, created_at, updated_at
                ) values (
                    :profileName, :ruleWeight, :graphWeight, :anomalyWeight,
                    :behaviorWeight, :enabled, :createdAt, :updatedAt
                )
                on conflict (profile_name) do update set
                    rule_weight = excluded.rule_weight,
                    graph_weight = excluded.graph_weight,
                    anomaly_weight = excluded.anomaly_weight,
                    behavior_weight = excluded.behavior_weight,
                    enabled = excluded.enabled,
                    updated_at = excluded.updated_at
                """;
        return jdbcTemplate.update(sql, params(profile));
    }

    public Optional<RiskWeightProfile> findProfileByName(String profileName) {
        String sql = "select * from risk_weight_profile where profile_name = :profileName and enabled = true";
        List<RiskWeightProfile> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("profileName", profileName), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<RiskWeightProfile> findEnabledProfiles() {
        return jdbcTemplate.query("select * from risk_weight_profile where enabled = true order by profile_name", ROW_MAPPER);
    }

    private MapSqlParameterSource params(RiskWeightProfile profile) {
        return new MapSqlParameterSource()
                .addValue("profileName", profile.profileName())
                .addValue("ruleWeight", profile.ruleWeight())
                .addValue("graphWeight", profile.graphWeight())
                .addValue("anomalyWeight", profile.anomalyWeight())
                .addValue("behaviorWeight", profile.behaviorWeight())
                .addValue("enabled", profile.enabled())
                .addValue("createdAt", Timestamp.from(profile.createdAt()))
                .addValue("updatedAt", Timestamp.from(profile.updatedAt()));
    }

    private static final class RiskWeightProfileRowMapper implements RowMapper<RiskWeightProfile> {
        @Override
        public RiskWeightProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskWeightProfile(
                    rs.getString("profile_name"),
                    rs.getDouble("rule_weight"),
                    rs.getDouble("graph_weight"),
                    rs.getDouble("anomaly_weight"),
                    rs.getDouble("behavior_weight"),
                    rs.getBoolean("enabled"),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
