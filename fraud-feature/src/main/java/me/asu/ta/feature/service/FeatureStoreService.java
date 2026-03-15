package me.asu.ta.feature.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.asu.ta.feature.compute.FeatureCalculator;
import me.asu.ta.feature.model.AccountFeatureHistory;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureHistoryRepository;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import org.springframework.stereotype.Service;

@Service
public class FeatureStoreService {
    private static final int DEFAULT_FEATURE_VERSION = 1;

    private final AccountFeatureSnapshotRepository snapshotRepository;
    private final AccountFeatureHistoryRepository historyRepository;
    private final List<FeatureCalculator> calculators;

    public FeatureStoreService(
            AccountFeatureSnapshotRepository snapshotRepository,
            AccountFeatureHistoryRepository historyRepository,
            List<FeatureCalculator> calculators) {
        this.snapshotRepository = snapshotRepository;
        this.historyRepository = historyRepository;
        this.calculators = List.copyOf(calculators);
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
                initializeBuilders(accountIds, generatedAt, DEFAULT_FEATURE_VERSION);
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

    public int persistSnapshot(AccountFeatureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshotRepository.save(snapshot);
    }

    public int persistHistory(AccountFeatureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return historyRepository.save(toHistory(snapshot));
    }

    private Map<String, AccountFeatureSnapshot.Builder> initializeBuilders(
            List<String> accountIds,
            Instant generatedAt,
            int featureVersion) {
        Map<String, AccountFeatureSnapshot.Builder> builders = new LinkedHashMap<>();
        for (String accountId : accountIds) {
            builders.put(accountId, AccountFeatureSnapshot.builder(accountId, generatedAt).featureVersion(featureVersion));
        }
        return builders;
    }

    private AccountFeatureHistory toHistory(AccountFeatureSnapshot snapshot) {
        return new AccountFeatureHistory(
                0L,
                snapshot.generatedAt(),
                snapshot.accountId(),
                snapshot.featureVersion(),
                snapshot.accountAgeDays(),
                snapshot.kycLevelNumeric(),
                snapshot.registrationIpRiskScore(),
                snapshot.loginCount24h(),
                snapshot.loginFailureCount24h(),
                snapshot.loginFailureRate24h(),
                snapshot.uniqueIpCount24h(),
                snapshot.highRiskIpLoginCount24h(),
                snapshot.vpnIpLoginCount24h(),
                snapshot.newDeviceLoginCount7d(),
                snapshot.nightLoginRatio7d(),
                snapshot.transactionCount24h(),
                snapshot.totalAmount24h(),
                snapshot.avgTransactionAmount24h(),
                snapshot.depositCount24h(),
                snapshot.withdrawCount24h(),
                snapshot.depositAmount24h(),
                snapshot.withdrawAmount24h(),
                snapshot.depositWithdrawRatio24h(),
                snapshot.uniqueCounterpartyCount24h(),
                snapshot.withdrawAfterDepositDelayAvg24h(),
                snapshot.rapidWithdrawAfterDepositFlag24h(),
                snapshot.rewardTransactionCount30d(),
                snapshot.rewardWithdrawDelayAvg30d(),
                snapshot.uniqueDeviceCount7d(),
                snapshot.deviceSwitchCount24h(),
                snapshot.sharedDeviceAccounts7d(),
                snapshot.securityEventCount24h(),
                snapshot.rapidProfileChangeFlag24h(),
                snapshot.securityChangeBeforeWithdrawFlag24h(),
                snapshot.sharedIpAccounts7d(),
                snapshot.sharedBankAccounts30d(),
                snapshot.graphClusterSize30d(),
                snapshot.riskNeighborCount30d(),
                snapshot.anomalyScoreLast());
    }
}
