package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Feature metadata mapped to the {@code feature_definition} table.
 *
 * @param featureName feature_definition.feature_name
 * @param entityType feature_definition.entity_type
 * @param dataType feature_definition.data_type
 * @param category feature_definition.category
 * @param description feature_definition.description
 * @param windowType feature_definition.window_type
 * @param windowValue feature_definition.window_value
 * @param computationMode feature_definition.computation_mode
 * @param sourceTables feature_definition.source_tables
 * @param ownerModule feature_definition.owner_module
 * @param featureVersion feature_definition.feature_version
 * @param active feature_definition.is_active
 * @param onlineServing feature_definition.is_online_serving
 * @param mlFeature feature_definition.is_ml_feature
 * @param ruleFeature feature_definition.is_rule_feature
 * @param createdAt feature_definition.created_at
 * @param updatedAt feature_definition.updated_at
 */
public record FeatureDefinition(
        String featureName,
        String entityType,
        String dataType,
        String category,
        String description,
        String windowType,
        Integer windowValue,
        String computationMode,
        String sourceTables,
        String ownerModule,
        int featureVersion,
        boolean active,
        boolean onlineServing,
        boolean mlFeature,
        boolean ruleFeature,
        Instant createdAt,
        Instant updatedAt
) {
    public FeatureDefinition {
        Objects.requireNonNull(featureName, "featureName");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(dataType, "dataType");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(computationMode, "computationMode");
        Objects.requireNonNull(sourceTables, "sourceTables");
        Objects.requireNonNull(ownerModule, "ownerModule");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
