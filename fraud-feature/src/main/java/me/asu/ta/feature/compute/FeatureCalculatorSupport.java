package me.asu.ta.feature.compute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class FeatureCalculatorSupport {
    private FeatureCalculatorSupport() {
    }

    static Map<String, AccountFeatureSnapshot.Builder> initializeBuilders(
            List<String> accountIds,
            Instant generatedAt,
            int featureVersion) {
        Map<String, AccountFeatureSnapshot.Builder> builders = new LinkedHashMap<>();
        for (String accountId : accountIds) {
            builders.put(accountId, AccountFeatureSnapshot.builder(accountId, generatedAt).featureVersion(featureVersion));
        }
        return builders;
    }

    static MapSqlParameterSource batchParams(List<String> accountIds, Instant generatedAt, Duration window) {
        return new MapSqlParameterSource()
                .addValue("accountIds", accountIds)
                .addValue("generatedAt", java.sql.Timestamp.from(generatedAt))
                .addValue("windowStart", java.sql.Timestamp.from(generatedAt.minus(window)));
    }

    static Integer getInteger(ResultSet rs, String column) throws SQLException {
        return (Integer) rs.getObject(column);
    }

    static Double getDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).doubleValue();
    }

    static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        return (Boolean) rs.getObject(column);
    }
}
