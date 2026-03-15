package me.asu.ta.feature.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.FeatureDefinition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeatureDefinitionRepository {
    private static final RowMapper<FeatureDefinition> ROW_MAPPER = new FeatureDefinitionRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FeatureDefinitionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(FeatureDefinition definition) {
        String sql = """
                insert into feature_definition(
                    feature_name, entity_type, data_type, category, description,
                    window_type, window_value, computation_mode, source_tables, owner_module,
                    feature_version, is_active, is_online_serving, is_ml_feature, is_rule_feature,
                    created_at, updated_at
                ) values (
                    :featureName, :entityType, :dataType, :category, :description,
                    :windowType, :windowValue, :computationMode, :sourceTables, :ownerModule,
                    :featureVersion, :active, :onlineServing, :mlFeature, :ruleFeature,
                    :createdAt, :updatedAt
                )
                on conflict (feature_name) do nothing
                """;
        return jdbcTemplate.update(sql, params(definition));
    }

    public int update(FeatureDefinition definition) {
        String sql = """
                update feature_definition
                   set entity_type = :entityType,
                       data_type = :dataType,
                       category = :category,
                       description = :description,
                       window_type = :windowType,
                       window_value = :windowValue,
                       computation_mode = :computationMode,
                       source_tables = :sourceTables,
                       owner_module = :ownerModule,
                       feature_version = :featureVersion,
                       is_active = :active,
                       is_online_serving = :onlineServing,
                       is_ml_feature = :mlFeature,
                       is_rule_feature = :ruleFeature,
                       updated_at = :updatedAt
                 where feature_name = :featureName
                """;
        return jdbcTemplate.update(sql, params(definition));
    }

    public Optional<FeatureDefinition> findLatestByAccountId(String accountId) {
        return Optional.empty();
    }

    public Optional<FeatureDefinition> findById(String featureName) {
        String sql = "select * from feature_definition where feature_name = :featureName";
        List<FeatureDefinition> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("featureName", featureName), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<FeatureDefinition> findBatch(List<String> featureNames) {
        String sql = "select * from feature_definition where feature_name in (:featureNames) order by feature_name";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("featureNames", featureNames), ROW_MAPPER);
    }

    public List<FeatureDefinition> findAll() {
        return jdbcTemplate.query("select * from feature_definition order by category, feature_name", ROW_MAPPER);
    }

    public int insertBatch(List<FeatureDefinition> definitions) {
        String sql = """
                insert into feature_definition(
                    feature_name, entity_type, data_type, category, description,
                    window_type, window_value, computation_mode, source_tables, owner_module,
                    feature_version, is_active, is_online_serving, is_ml_feature, is_rule_feature,
                    created_at, updated_at
                ) values (
                    :featureName, :entityType, :dataType, :category, :description,
                    :windowType, :windowValue, :computationMode, :sourceTables, :ownerModule,
                    :featureVersion, :active, :onlineServing, :mlFeature, :ruleFeature,
                    :createdAt, :updatedAt
                )
                on conflict (feature_name) do update set
                    entity_type = excluded.entity_type,
                    data_type = excluded.data_type,
                    category = excluded.category,
                    description = excluded.description,
                    window_type = excluded.window_type,
                    window_value = excluded.window_value,
                    computation_mode = excluded.computation_mode,
                    source_tables = excluded.source_tables,
                    owner_module = excluded.owner_module,
                    feature_version = excluded.feature_version,
                    is_active = excluded.is_active,
                    is_online_serving = excluded.is_online_serving,
                    is_ml_feature = excluded.is_ml_feature,
                    is_rule_feature = excluded.is_rule_feature,
                    created_at = excluded.created_at,
                    updated_at = excluded.updated_at
                """;
        return jdbcTemplate.batchUpdate(sql, definitions.stream().map(this::params).toArray(MapSqlParameterSource[]::new)).length;
    }

    private MapSqlParameterSource params(FeatureDefinition definition) {
        return new MapSqlParameterSource()
                .addValue("featureName", definition.featureName())
                .addValue("entityType", definition.entityType())
                .addValue("dataType", definition.dataType())
                .addValue("category", definition.category())
                .addValue("description", definition.description())
                .addValue("windowType", definition.windowType())
                .addValue("windowValue", definition.windowValue())
                .addValue("computationMode", definition.computationMode())
                .addValue("sourceTables", definition.sourceTables())
                .addValue("ownerModule", definition.ownerModule())
                .addValue("featureVersion", definition.featureVersion())
                .addValue("active", definition.active())
                .addValue("onlineServing", definition.onlineServing())
                .addValue("mlFeature", definition.mlFeature())
                .addValue("ruleFeature", definition.ruleFeature())
                .addValue("createdAt", Timestamp.from(definition.createdAt()))
                .addValue("updatedAt", Timestamp.from(definition.updatedAt()));
    }

    private static final class FeatureDefinitionRowMapper implements RowMapper<FeatureDefinition> {
        @Override
        public FeatureDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FeatureDefinition(
                    rs.getString("feature_name"),
                    rs.getString("entity_type"),
                    rs.getString("data_type"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("window_type"),
                    (Integer) rs.getObject("window_value"),
                    rs.getString("computation_mode"),
                    rs.getString("source_tables"),
                    rs.getString("owner_module"),
                    rs.getInt("feature_version"),
                    rs.getBoolean("is_active"),
                    rs.getBoolean("is_online_serving"),
                    rs.getBoolean("is_ml_feature"),
                    rs.getBoolean("is_rule_feature"),
                    toInstant(rs.getTimestamp("created_at")),
                    toInstant(rs.getTimestamp("updated_at")));
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
