package me.asu.ta.feature.service;

import java.util.List;
import java.util.Objects;
import me.asu.ta.feature.model.AccountFeatureHistory;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureHistoryRepository;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import org.springframework.stereotype.Service;

/**
 * Persists feature snapshots and histories.
 * Risk evaluation and case generation should not be orchestrated here.
 */
@Service
public class FeaturePersistenceService {
    private final AccountFeatureSnapshotRepository snapshotRepository;
    private final AccountFeatureHistoryRepository historyRepository;

    public FeaturePersistenceService(
            AccountFeatureSnapshotRepository snapshotRepository,
            AccountFeatureHistoryRepository historyRepository) {
        this.snapshotRepository = snapshotRepository;
        this.historyRepository = historyRepository;
    }

    public int persistSnapshot(AccountFeatureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshotRepository.save(snapshot);
    }

    public int persistHistory(AccountFeatureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return historyRepository.save(toHistory(snapshot));
    }

    public int persistBatch(List<AccountFeatureSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots");
        int persisted = 0;
        for (AccountFeatureSnapshot snapshot : snapshots) {
            persistSnapshot(snapshot);
            persistHistory(snapshot);
            persisted++;
        }
        return persisted;
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
                snapshot.riskNeighborCount30d());
    }
}
