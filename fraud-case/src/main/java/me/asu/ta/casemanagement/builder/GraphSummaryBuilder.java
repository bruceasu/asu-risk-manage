package me.asu.ta.casemanagement.builder;

import java.time.Instant;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.GraphRiskSignal;
import org.springframework.stereotype.Component;

@Component
public class GraphSummaryBuilder {
    public CaseGraphSummary build(long caseId, CaseGenerationRequest request, Instant createdAt) {
        GraphRiskSignal signal = resolveGraphSignal(request.snapshot(), request.graphRiskSignal());
        return new CaseGraphSummary(
                caseId,
                signal.graphScore(),
                signal.graphClusterSize(),
                signal.riskNeighborCount(),
                signal.sharedDeviceAccounts(),
                signal.sharedBankAccounts(),
                createdAt);
    }

    public GraphRiskSignal resolveGraphSignal(AccountFeatureSnapshot snapshot, GraphRiskSignal signal) {
        if (signal != null) {
            return signal;
        }
        double score = 0.0d;
        if (intValue(snapshot.riskNeighborCount30d()) >= 3) {
            score += 40.0d;
        }
        if (intValue(snapshot.graphClusterSize30d()) >= 5) {
            score += 30.0d;
        }
        if (intValue(snapshot.sharedDeviceAccounts7d()) >= 5) {
            score += 15.0d;
        }
        if (intValue(snapshot.sharedBankAccounts30d()) >= 3) {
            score += 15.0d;
        }
        return new GraphRiskSignal(
                Math.min(score, 100.0d),
                intValue(snapshot.graphClusterSize30d()),
                intValue(snapshot.riskNeighborCount30d()),
                intValue(snapshot.sharedDeviceAccounts7d()),
                intValue(snapshot.sharedBankAccounts30d()));
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}
