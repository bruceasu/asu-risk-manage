package me.asu.ta.feature.compute;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;

/**
 * Batch feature calculator contract for populating snapshot builders.
 */
public interface FeatureCalculator {

    /**
     * Enriches existing snapshot builders for the given account batch.
     *
     * @param accountIds target account identifiers
     * @param generatedAt snapshot generation time
     * @param featureVersion snapshot feature version
     * @param builders mutable snapshot builders keyed by account id
     */
    void enrichBatch(
            List<String> accountIds,
            Instant generatedAt,
            int featureVersion,
            Map<String, AccountFeatureSnapshot.Builder> builders);
}
