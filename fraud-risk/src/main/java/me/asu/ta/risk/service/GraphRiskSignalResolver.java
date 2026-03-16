package me.asu.ta.risk.service;

import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import org.springframework.stereotype.Component;

@Component
public class GraphRiskSignalResolver {
    public GraphRiskSignal resolve(RiskEvaluationRequest request) {
        if (request.graphRiskSignal() != null) {
            return request.graphRiskSignal();
        }
        return resolve(request.snapshot());
    }

    public GraphRiskSignal resolve(AccountFeatureSnapshot snapshot) {
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

    public Map<String, Object> toContextMap(GraphRiskSignal signal) {
        return Map.of(
                "graphScore", signal.graphScore(),
                "graphClusterSize", signal.graphClusterSize(),
                "riskNeighborCount", signal.riskNeighborCount(),
                "sharedDeviceAccounts", signal.sharedDeviceAccounts(),
                "sharedBankAccounts", signal.sharedBankAccounts());
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}
