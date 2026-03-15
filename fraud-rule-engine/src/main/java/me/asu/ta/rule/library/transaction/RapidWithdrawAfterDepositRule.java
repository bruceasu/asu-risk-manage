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
public class RapidWithdrawAfterDepositRule implements FraudRule {
    private static final String RULE_CODE = "RAPID_WITHDRAW_AFTER_DEPOSIT";

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
        int maxDelayMinutes = RuleSupport.intParam(config, "maxDelayMinutes", 30);
        int minDepositCount24h = RuleSupport.intParam(config, "minDepositCount24h", 1);
        int minWithdrawCount24h = RuleSupport.intParam(config, "minWithdrawCount24h", 1);
        int depositCount = RuleSupport.intValue(snapshot.depositCount24h());
        int withdrawCount = RuleSupport.intValue(snapshot.withdrawCount24h());
        double delayMinutes = RuleSupport.doubleValue(snapshot.withdrawAfterDepositDelayAvg24h());
        boolean rapidFlag = RuleSupport.booleanValue(snapshot.rapidWithdrawAfterDepositFlag24h());
        boolean hit = depositCount >= minDepositCount24h
                && withdrawCount >= minWithdrawCount24h
                && (rapidFlag || delayMinutes <= maxDelayMinutes);
        return RuleSupport.result(
                RULE_CODE,
                config.version(),
                hit,
                config.severity(),
                hit ? config.scoreWeight() : 0,
                "RAPID_WITHDRAW_AFTER_DEPOSIT",
                hit ? "Rapid withdraw after deposit detected" : "Rapid withdraw after deposit not detected",
                RuleSupport.evidence(
                        "depositCount24h", depositCount,
                        "withdrawCount24h", withdrawCount,
                        "withdrawAfterDepositDelayAvg24h", delayMinutes,
                        "rapidWithdrawAfterDepositFlag24h", rapidFlag,
                        "maxDelayMinutes", maxDelayMinutes));
    }
}
