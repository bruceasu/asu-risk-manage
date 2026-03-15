package me.asu.ta.risk.scoring;

import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskWeightProfile;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.rule.model.RuleEngineResult;
import org.springframework.stereotype.Component;

@Component
public class RiskScoreCalculator {
    public ScoreBreakdown calculate(
            RuleEngineResult ruleEngineResult,
            GraphRiskSignal graphRiskSignal,
            MlAnomalySignal mlAnomalySignal,
            BehaviorRiskSignal behaviorRiskSignal,
            RiskWeightProfile profile) {
        double ruleScore = clamp(ruleEngineResult == null ? 0.0d : ruleEngineResult.totalScore());
        double graphScore = clamp(graphRiskSignal == null ? 0.0d : graphRiskSignal.graphScore());
        double anomalyScore = clamp(mlAnomalySignal == null ? 0.0d : mlAnomalySignal.anomalyScoreNormalized());
        double behaviorScore = clamp(behaviorRiskSignal == null ? 0.0d : behaviorRiskSignal.behaviorScore());

        double finalScore = (ruleScore * profile.ruleWeight())
                + (graphScore * profile.graphWeight())
                + (anomalyScore * profile.anomalyWeight())
                + (behaviorScore * profile.behaviorWeight());

        return new ScoreBreakdown(
                ruleScore,
                graphScore,
                anomalyScore,
                behaviorScore,
                round(clamp(finalScore)),
                profile.profileName());
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(100.0d, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
