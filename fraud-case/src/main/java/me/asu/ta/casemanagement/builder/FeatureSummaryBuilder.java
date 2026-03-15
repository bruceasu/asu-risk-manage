package me.asu.ta.casemanagement.builder;

import java.time.Instant;
import me.asu.ta.casemanagement.model.CaseFeatureSummary;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import org.springframework.stereotype.Component;

@Component
public class FeatureSummaryBuilder {
    public CaseFeatureSummary build(long caseId, AccountFeatureSnapshot snapshot, Instant createdAt) {
        return new CaseFeatureSummary(
                caseId,
                snapshot.accountAgeDays(),
                snapshot.highRiskIpLoginCount24h(),
                snapshot.loginFailureRate24h(),
                snapshot.newDeviceLoginCount7d(),
                snapshot.withdrawAfterDepositDelayAvg24h(),
                snapshot.sharedDeviceAccounts7d(),
                snapshot.securityChangeBeforeWithdrawFlag24h(),
                snapshot.graphClusterSize30d(),
                snapshot.riskNeighborCount30d(),
                snapshot.anomalyScoreLast(),
                createdAt);
    }
}
