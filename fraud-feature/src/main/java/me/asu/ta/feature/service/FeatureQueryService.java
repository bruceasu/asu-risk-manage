package me.asu.ta.feature.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import org.springframework.stereotype.Service;

/**
 * Read-only access to persisted features.
 */
@Service
public class FeatureQueryService {
    private final AccountFeatureSnapshotRepository snapshotRepository;

    public FeatureQueryService(AccountFeatureSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public Optional<AccountFeatureSnapshot> getLatestFeatures(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return snapshotRepository.findLatestByAccountId(accountId);
    }

    public Map<String, AccountFeatureSnapshot> getLatestFeaturesBatch(List<String> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        Map<String, AccountFeatureSnapshot> results = new LinkedHashMap<>();
        if (accountIds.isEmpty()) {
            return results;
        }
        for (AccountFeatureSnapshot snapshot : snapshotRepository.findBatch(accountIds)) {
            results.put(snapshot.accountId(), snapshot);
        }
        return results;
    }
}
