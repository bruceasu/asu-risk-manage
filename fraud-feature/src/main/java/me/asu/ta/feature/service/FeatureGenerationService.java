package me.asu.ta.feature.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.feature.compute.FeatureCalculator;
import me.asu.ta.feature.compute.FeatureCalculatorSupport;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.stereotype.Service;

/**
 * Generates feature snapshots only.
 * Persistence and downstream orchestration must stay outside this service.
 */
@Service
public class FeatureGenerationService {
    private static final int DEFAULT_FEATURE_VERSION = 1;

    private final List<FeatureCalculator> calculators;

    public FeatureGenerationService(List<FeatureCalculator> calculators) {
        this.calculators = List.copyOf(calculators);
    }

    public AccountFeatureSnapshot generateFeaturesForAccount(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        List<AccountFeatureSnapshot> snapshots = generateFeaturesBatch(List.of(accountId));
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("No feature snapshot generated for accountId=" + accountId);
        }
        return snapshots.getFirst();
    }

    public List<AccountFeatureSnapshot> generateFeaturesBatch(List<String> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        if (accountIds.isEmpty()) {
            return List.of();
        }
        Instant generatedAt = Instant.now();
        Map<String, AccountFeatureSnapshot.Builder> builders =
                FeatureCalculatorSupport.initializeBuilders(accountIds, generatedAt, DEFAULT_FEATURE_VERSION);
        for (FeatureCalculator calculator : calculators) {
            calculator.enrichBatch(accountIds, generatedAt, DEFAULT_FEATURE_VERSION, builders);
        }
        List<AccountFeatureSnapshot> snapshots = new ArrayList<>(builders.size());
        for (String accountId : accountIds) {
            AccountFeatureSnapshot.Builder builder = builders.get(accountId);
            if (builder != null) {
                snapshots.add(builder.build());
            }
        }
        return snapshots;
    }
}
