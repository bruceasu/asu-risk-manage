package me.asu.ta.offline.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import me.asu.ta.risk.model.GraphRiskSignal;
import org.springframework.stereotype.Service;

@Service
public class OfflineGraphBridgeService {
    public Map<String, GraphRiskSignal> buildGraphSignals(OfflineAnalysisBundle bundle) {
        Map<String, GraphRiskSignal> signals = new LinkedHashMap<>();
        for (var entry : bundle.replayState().getAggByAccount().entrySet()) {
            String accountId = entry.getKey();
            int clusterSize = bundle.clusterSizes().getOrDefault(accountId, 1);
            int riskNeighborCount = Math.max(0, clusterSize - 1);
            double graphScore = 0.0d;
            if (riskNeighborCount >= 3) {
                graphScore += 40.0d;
            }
            if (clusterSize >= 5) {
                graphScore += 30.0d;
            }
            signals.put(accountId, new GraphRiskSignal(Math.min(graphScore, 100.0d), clusterSize, riskNeighborCount, 0, 0));
        }
        return signals;
    }
}
