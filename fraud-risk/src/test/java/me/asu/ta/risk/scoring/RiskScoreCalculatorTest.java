package me.asu.ta.risk.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineResult;
import org.junit.jupiter.api.Test;

class RiskScoreCalculatorTest {
    private final RiskScoreCalculator calculator = new RiskScoreCalculator();

    @Test
    void shouldCalculateScoreWithDefaultProfile() {
        ScoreBreakdown breakdown = calculator.calculate(
                new RuleEngineResult("acct-risk-1", EvaluationMode.REALTIME, RiskTestSupport.FIXED_TIME, 70, List.of(), List.of()),
                new GraphRiskSignal(60.0d, 6, 4, 5, 2),
                new MlAnomalySignal(0.87d, 87.0d, "risk-model", RiskTestSupport.FIXED_TIME),
                new BehaviorRiskSignal(80.0d, java.util.Map.of(), List.of()),
                RiskTestSupport.defaultProfile());

        assertEquals(70.0d, breakdown.ruleScore());
        assertEquals(60.0d, breakdown.graphScore());
        assertEquals(87.0d, breakdown.anomalyScore());
        assertEquals(80.0d, breakdown.behaviorScore());
        assertEquals(72.4d, breakdown.finalScore());
        assertEquals("DEFAULT", breakdown.profileName());
    }

    @Test
    void shouldCalculateScoreWithNoMlProfile() {
        ScoreBreakdown breakdown = calculator.calculate(
                new RuleEngineResult("acct-risk-2", EvaluationMode.BATCH, RiskTestSupport.FIXED_TIME, 70, List.of(), List.of()),
                new GraphRiskSignal(60.0d, 6, 4, 5, 2),
                null,
                new BehaviorRiskSignal(80.0d, java.util.Map.of(), List.of()),
                RiskTestSupport.noMlProfile());

        assertEquals(68.5d, breakdown.finalScore());
        assertEquals("NO_ML", breakdown.profileName());
    }
}
