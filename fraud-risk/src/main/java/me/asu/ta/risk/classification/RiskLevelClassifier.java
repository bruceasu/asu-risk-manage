package me.asu.ta.risk.classification;

import me.asu.ta.risk.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class RiskLevelClassifier {
    public RiskLevel classify(double riskScore) {
        if (riskScore >= 80.0d) {
            return RiskLevel.CRITICAL;
        }
        if (riskScore >= 60.0d) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= 30.0d) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
