package me.asu.ta.risk.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.OfflineBehaviorContextKeys;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import org.springframework.stereotype.Component;

@Component
public class BehaviorScoreCalculator {
    public BehaviorRiskSignal calculate(AccountFeatureSnapshot snapshot, Map<String, Object> contextSignals) {
        double score = 0.0d;
        Map<String, Object> evidence = new LinkedHashMap<>();
        List<String> reasonCodes = new ArrayList<>();

        double loginFailureRate = doubleValue(snapshot.loginFailureRate24h());
        if (loginFailureRate > BehaviorScorePolicy.LOGIN_FAILURE_RATE_HIGH.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.LOGIN_FAILURE_RATE_HIGH, loginFailureRate);
        }

        int highRiskIpLoginCount = intValue(snapshot.highRiskIpLoginCount24h());
        if (highRiskIpLoginCount >= BehaviorScorePolicy.HIGH_RISK_IP_ACTIVITY.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.HIGH_RISK_IP_ACTIVITY, highRiskIpLoginCount);
        }

        double withdrawDelay = doubleValue(snapshot.withdrawAfterDepositDelayAvg24h());
        if (withdrawDelay > 0.0d && withdrawDelay <= BehaviorScorePolicy.RAPID_WITHDRAW_PATTERN.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.RAPID_WITHDRAW_PATTERN, withdrawDelay);
        }

        int sharedDeviceAccounts = intValue(snapshot.sharedDeviceAccounts7d());
        if (sharedDeviceAccounts >= BehaviorScorePolicy.SHARED_DEVICE_EXPOSURE.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.SHARED_DEVICE_EXPOSURE, sharedDeviceAccounts);
        }

        if (Boolean.TRUE.equals(snapshot.securityChangeBeforeWithdrawFlag24h())) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.SECURITY_CHANGE_BEFORE_WITHDRAW, true);
        }

        int behaviorClusterSize = intValue(contextSignals.get(OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE));
        if (behaviorClusterSize >= BehaviorScorePolicy.OFFLINE_CLUSTER_DENSE.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.OFFLINE_CLUSTER_DENSE, behaviorClusterSize);
        }

        int similarAccountCount = intValue(contextSignals.get(OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT));
        if (similarAccountCount >= BehaviorScorePolicy.OFFLINE_SIMILAR_ACCOUNTS.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.OFFLINE_SIMILAR_ACCOUNTS, similarAccountCount);
        }

        double coordinatedTradingScore = doubleValue(contextSignals.get(OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE));
        if (coordinatedTradingScore >= BehaviorScorePolicy.OFFLINE_COORDINATED_TRADING.threshold()) {
            score += apply(evidence, reasonCodes, BehaviorScorePolicy.OFFLINE_COORDINATED_TRADING, coordinatedTradingScore);
        }

        return new BehaviorRiskSignal(Math.min(score, 100.0d), evidence, reasonCodes);
    }

    private double apply(
            Map<String, Object> evidence,
            List<String> reasonCodes,
            BehaviorScorePolicy.SignalRule rule,
            Object evidenceValue) {
        reasonCodes.add(rule.reasonCode());
        evidence.put(rule.evidenceKey(), evidenceValue);
        return rule.score();
    }

    private int intValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private double doubleValue(Object value) {
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0d;
    }
}
