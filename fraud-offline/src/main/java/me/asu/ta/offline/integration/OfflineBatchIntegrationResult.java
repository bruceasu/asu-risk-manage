package me.asu.ta.offline.integration;

import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.RiskScoreResult;

public record OfflineBatchIntegrationResult(
        List<AccountFeatureSnapshot> snapshots,
        Map<String, RiskScoreResult> riskResults
) {
}
