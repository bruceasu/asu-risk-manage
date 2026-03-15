package me.asu.ta.rule.library.graph;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class HighRiskNeighborClusterRule implements FraudRule {
    private static final String RULE_CODE = "HIGH_RISK_NEIGHBOR_CLUSTER";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.GRAPH;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        int minRiskNeighborCount30d = RuleSupport.intParam(config, "minRiskNeighborCount30d", 3);
        int minGraphClusterSize30d = RuleSupport.intParam(config, "minGraphClusterSize30d", 5);
        int riskNeighborCount = RuleSupport.intValue(snapshot.riskNeighborCount30d());
        int graphClusterSize = RuleSupport.intValue(snapshot.graphClusterSize30d());
        boolean hit = riskNeighborCount >= minRiskNeighborCount30d && graphClusterSize >= minGraphClusterSize30d;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "HIGH_RISK_NEIGHBOR_CLUSTER",
                hit ? "High risk neighbor cluster detected" : "High risk neighbor cluster not detected",
                RuleSupport.evidence(
                        "riskNeighborCount30d", riskNeighborCount,
                        "graphClusterSize30d", graphClusterSize,
                        "minRiskNeighborCount30d", minRiskNeighborCount30d,
                        "minGraphClusterSize30d", minGraphClusterSize30d));
    }
}
