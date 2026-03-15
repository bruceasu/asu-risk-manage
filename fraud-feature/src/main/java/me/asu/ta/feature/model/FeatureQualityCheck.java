package me.asu.ta.feature.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Quality check result mapped to the {@code feature_quality_check} table.
 *
 * @param checkId feature_quality_check.check_id
 * @param checkTime feature_quality_check.check_time
 * @param featureVersion feature_quality_check.feature_version
 * @param featureName feature_quality_check.feature_name
 * @param checkType feature_quality_check.check_type
 * @param status feature_quality_check.status
 * @param totalRecords feature_quality_check.total_records
 * @param failedRecords feature_quality_check.failed_records
 * @param details feature_quality_check.details
 */
public record FeatureQualityCheck(
        long checkId,
        Instant checkTime,
        int featureVersion,
        String featureName,
        String checkType,
        String status,
        Integer totalRecords,
        Integer failedRecords,
        String details
) {
    public FeatureQualityCheck {
        Objects.requireNonNull(checkTime, "checkTime");
        Objects.requireNonNull(featureName, "featureName");
        Objects.requireNonNull(checkType, "checkType");
        Objects.requireNonNull(status, "status");
    }
}
