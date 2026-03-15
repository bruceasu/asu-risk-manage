package me.asu.ta.risk.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import org.springframework.stereotype.Component;

@Component
public class BehaviorScoreCalculator {
    public BehaviorRiskSignal calculate(AccountFeatureSnapshot snapshot) {
        double score = 0.0d;
        Map<String, Object> evidence = new LinkedHashMap<>();
        List<String> reasonCodes = new ArrayList<>();

        double loginFailureRate = doubleValue(snapshot.loginFailureRate24h());
        if (loginFailureRate > 0.8d) {
            score += 15.0d;
            reasonCodes.add("BEHAVIOR_LOGIN_FAILURE_RATE_HIGH");
            evidence.put("loginFailureRate24h", loginFailureRate);
        }

        int highRiskIpLoginCount = intValue(snapshot.highRiskIpLoginCount24h());
        if (highRiskIpLoginCount >= 1) {
            score += 20.0d;
            reasonCodes.add("BEHAVIOR_HIGH_RISK_IP_ACTIVITY");
            evidence.put("highRiskIpLoginCount24h", highRiskIpLoginCount);
        }

        double withdrawDelay = doubleValue(snapshot.withdrawAfterDepositDelayAvg24h());
        if (withdrawDelay > 0.0d && withdrawDelay <= 30.0d) {
            score += 20.0d;
            reasonCodes.add("BEHAVIOR_RAPID_WITHDRAW_PATTERN");
            evidence.put("withdrawAfterDepositDelayAvg24h", withdrawDelay);
        }

        int sharedDeviceAccounts = intValue(snapshot.sharedDeviceAccounts7d());
        if (sharedDeviceAccounts >= 5) {
            score += 20.0d;
            reasonCodes.add("BEHAVIOR_SHARED_DEVICE_EXPOSURE");
            evidence.put("sharedDeviceAccounts7d", sharedDeviceAccounts);
        }

        if (Boolean.TRUE.equals(snapshot.securityChangeBeforeWithdrawFlag24h())) {
            score += 25.0d;
            reasonCodes.add("BEHAVIOR_SECURITY_CHANGE_BEFORE_WITHDRAW");
            evidence.put("securityChangeBeforeWithdrawFlag24h", true);
        }

        return new BehaviorRiskSignal(Math.min(score, 100.0d), evidence, reasonCodes);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private double doubleValue(Double value) {
        return value == null ? 0.0d : value;
    }
}
