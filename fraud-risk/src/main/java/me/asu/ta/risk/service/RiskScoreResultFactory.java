package me.asu.ta.risk.service;

import java.util.List;
import me.asu.ta.risk.classification.RiskLevelClassifier;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.RiskWeightProfile;
import me.asu.ta.risk.model.ScoreBreakdown;
import org.springframework.stereotype.Component;

@Component
public class RiskScoreResultFactory {
    private final RiskLevelClassifier riskLevelClassifier;

    public RiskScoreResultFactory(RiskLevelClassifier riskLevelClassifier) {
        this.riskLevelClassifier = riskLevelClassifier;
    }

    public RiskScoreResult build(
            RiskEvaluationRequest request,
            ScoreBreakdown scoreBreakdown,
            RiskWeightProfile profile,
            List<String> reasonCodes) {
        RiskLevel riskLevel = riskLevelClassifier.classify(scoreBreakdown.finalScore());
        return new RiskScoreResult(
                0L,
                request.accountId(),
                scoreBreakdown.finalScore(),
                riskLevel,
                profile.profileName(),
                request.snapshot().featureVersion(),
                request.resolvedEvaluationTime(),
                request.resolvedEvaluationMode(),
                reasonCodes,
                scoreBreakdown);
    }
}
