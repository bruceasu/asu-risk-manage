package me.asu.ta.rule.library.composite;

import me.asu.ta.OfflineBehaviorContextKeys;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class OfflineCoordinatedTradingRule implements FraudRule {
    private static final String RULE_CODE = "OFFLINE_COORDINATED_TRADING";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.COMPOSITE;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        var params = RuleSupport.offlineBehaviorParams(config);
        double minCoordinatedTradingScore = params.minCoordinatedTradingScore() == null
                ? 60.0d
                : params.minCoordinatedTradingScore().doubleValue();
        int minSimilarAccountCount = params.minSimilarAccountCount() == null
                ? 3
                : params.minSimilarAccountCount().intValue();
        int minBehaviorClusterSize = params.minBehaviorClusterSize() == null
                ? 4
                : params.minBehaviorClusterSize().intValue();

        double coordinatedTradingScore = RuleSupport.doubleAttribute(
                context,
                OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE,
                0.0d);
        int similarAccountCount = RuleSupport.intAttribute(
                context,
                OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT,
                0);
        int behaviorClusterSize = RuleSupport.intAttribute(
                context,
                OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE,
                0);

        boolean hit = coordinatedTradingScore >= minCoordinatedTradingScore
                && similarAccountCount >= minSimilarAccountCount
                && behaviorClusterSize >= minBehaviorClusterSize;

        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "OFFLINE_COORDINATED_TRADING_SIGNAL",
                hit
                        ? "Offline behavior context indicates coordinated trading risk"
                        : "Offline behavior context does not meet coordinated trading thresholds",
                RuleSupport.evidence(
                        OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE, coordinatedTradingScore,
                        "minCoordinatedTradingScore", minCoordinatedTradingScore,
                        OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT, similarAccountCount,
                        "minSimilarAccountCount", minSimilarAccountCount,
                        OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE, behaviorClusterSize,
                        "minBehaviorClusterSize", minBehaviorClusterSize));
    }
}
