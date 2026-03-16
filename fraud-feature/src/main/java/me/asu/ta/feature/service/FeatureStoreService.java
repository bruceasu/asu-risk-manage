package me.asu.ta.feature.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.stereotype.Service;

/**
 * Compatibility facade for existing callers.
 * New code should prefer the narrower query, generation, and persistence services.
 */
@Service
public class FeatureStoreService {
    private final FeatureQueryService featureQueryService;
    private final FeatureGenerationService featureGenerationService;
    private final FeaturePersistenceService featurePersistenceService;

    public FeatureStoreService(
            FeatureQueryService featureQueryService,
            FeatureGenerationService featureGenerationService,
            FeaturePersistenceService featurePersistenceService) {
        this.featureQueryService = featureQueryService;
        this.featureGenerationService = featureGenerationService;
        this.featurePersistenceService = featurePersistenceService;
    }

    public Optional<AccountFeatureSnapshot> getLatestFeatures(String accountId) {
        return featureQueryService.getLatestFeatures(accountId);
    }

    public Map<String, AccountFeatureSnapshot> getLatestFeaturesBatch(List<String> accountIds) {
        return featureQueryService.getLatestFeaturesBatch(accountIds);
    }

    public AccountFeatureSnapshot generateFeaturesForAccount(String accountId) {
        return featureGenerationService.generateFeaturesForAccount(accountId);
    }

    public List<AccountFeatureSnapshot> generateFeaturesBatch(List<String> accountIds) {
        return featureGenerationService.generateFeaturesBatch(accountIds);
    }

    public int persistSnapshot(AccountFeatureSnapshot snapshot) {
        return featurePersistenceService.persistSnapshot(snapshot);
    }

    public int persistHistory(AccountFeatureSnapshot snapshot) {
        return featurePersistenceService.persistHistory(snapshot);
    }
}
