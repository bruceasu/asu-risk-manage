package me.asu.ta.feature.service;

import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.model.CaseReportFeatureView;
import me.asu.ta.feature.model.MlFeatureView;
import me.asu.ta.feature.model.RiskEngineFeatureView;
import org.springframework.stereotype.Service;

@Service
public class FeatureServiceAdapter {
    private final FeatureStoreService featureStoreService;

    public FeatureServiceAdapter(FeatureStoreService featureStoreService) {
        this.featureStoreService = featureStoreService;
    }

    public Optional<RiskEngineFeatureView> getFeaturesForRiskEngine(String accountId) {
        return featureStoreService.getLatestFeatures(accountId).map(this::toRiskEngineView);
    }

    public Optional<MlFeatureView> getFeaturesForML(String accountId) {
        return featureStoreService.getLatestFeatures(accountId).map(this::toMlFeatureView);
    }

    public Optional<CaseReportFeatureView> getFeaturesForCaseReport(String accountId) {
        return featureStoreService.getLatestFeatures(accountId).map(this::toCaseReportFeatureView);
    }

    private RiskEngineFeatureView toRiskEngineView(AccountFeatureSnapshot snapshot) {
        return new RiskEngineFeatureView(
                snapshot.accountId(),
                intValue(snapshot.loginFailureCount24h()),
                doubleValue(snapshot.loginFailureRate24h()),
                intValue(snapshot.highRiskIpLoginCount24h()),
                doubleValue(snapshot.depositWithdrawRatio24h()),
                booleanValue(snapshot.rapidWithdrawAfterDepositFlag24h()),
                intValue(snapshot.sharedDeviceAccounts7d()),
                booleanValue(snapshot.securityChangeBeforeWithdrawFlag24h()),
                intValue(snapshot.graphClusterSize30d()),
                intValue(snapshot.riskNeighborCount30d()),
                doubleValue(snapshot.anomalyScoreLast()));
    }

    private MlFeatureView toMlFeatureView(AccountFeatureSnapshot snapshot) {
        return new MlFeatureView(
                snapshot.accountId(),
                intValue(snapshot.loginCount24h()),
                intValue(snapshot.uniqueIpCount24h()),
                intValue(snapshot.uniqueDeviceCount7d()),
                intValue(snapshot.transactionCount24h()),
                doubleValue(snapshot.totalAmount24h()),
                doubleValue(snapshot.avgTransactionAmount24h()),
                doubleValue(snapshot.depositWithdrawRatio24h()),
                intValue(snapshot.deviceSwitchCount24h()),
                intValue(snapshot.securityEventCount24h()),
                intValue(snapshot.graphClusterSize30d()),
                doubleValue(snapshot.anomalyScoreLast()));
    }

    private CaseReportFeatureView toCaseReportFeatureView(AccountFeatureSnapshot snapshot) {
        return new CaseReportFeatureView(
                snapshot.accountId(),
                intValue(snapshot.accountAgeDays()),
                intValue(snapshot.highRiskIpLoginCount24h()),
                intValue(snapshot.uniqueDeviceCount7d()),
                booleanValue(snapshot.rapidWithdrawAfterDepositFlag24h()),
                intValue(snapshot.sharedDeviceAccounts7d()),
                intValue(snapshot.graphClusterSize30d()),
                intValue(snapshot.riskNeighborCount30d()),
                doubleValue(snapshot.anomalyScoreLast()));
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private double doubleValue(Double value) {
        return value == null ? 0.0d : value;
    }

    private boolean booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
