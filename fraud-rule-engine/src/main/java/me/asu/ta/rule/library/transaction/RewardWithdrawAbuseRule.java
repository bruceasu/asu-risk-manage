package me.asu.ta.rule.library.transaction;

import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.api.FraudRule;
import me.asu.ta.rule.library.RuleSupport;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class RewardWithdrawAbuseRule implements FraudRule {
    private static final String RULE_CODE = "REWARD_WITHDRAW_ABUSE";

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.TRANSACTION;
    }

    @Override
    public RuleEvaluationResult evaluate(RuleEvaluationContext context, RuleConfig config) {
        AccountFeatureSnapshot snapshot = RuleSupport.snapshot(context);
        int minRewardTransactionCount30d = RuleSupport.intParam(config, "minRewardTransactionCount30d", 1);
        double maxRewardWithdrawDelayHours30d = RuleSupport.doubleParam(config, "maxRewardWithdrawDelayHours30d", 24.0d);
        int rewardTransactionCount = RuleSupport.intValue(snapshot.rewardTransactionCount30d());
        double rewardWithdrawDelay = RuleSupport.doubleValue(snapshot.rewardWithdrawDelayAvg30d());
        boolean hit = rewardTransactionCount >= minRewardTransactionCount30d
                && rewardWithdrawDelay > 0.0d
                && rewardWithdrawDelay <= maxRewardWithdrawDelayHours30d;
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "REWARD_WITHDRAW_ABUSE",
                hit ? "Reward withdraw abuse pattern detected" : "Reward withdraw abuse pattern not detected",
                RuleSupport.evidence(
                        "rewardTransactionCount30d", rewardTransactionCount,
                        "rewardWithdrawDelayAvg30d", rewardWithdrawDelay,
                        "minRewardTransactionCount30d", minRewardTransactionCount30d,
                        "maxRewardWithdrawDelayHours30d", maxRewardWithdrawDelayHours30d));
    }
}
