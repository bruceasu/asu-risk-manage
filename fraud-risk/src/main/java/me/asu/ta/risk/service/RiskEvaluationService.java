package me.asu.ta.risk.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.risk.classification.RiskLevelClassifier;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.RiskWeightProfile;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.risk.reason.RiskReasonGenerator;
import me.asu.ta.risk.repository.RiskScoreResultRepository;
import me.asu.ta.risk.scoring.BehaviorScoreCalculator;
import me.asu.ta.risk.scoring.RiskScoreCalculator;
import me.asu.ta.risk.scoring.RiskWeightProfileService;
import me.asu.ta.rule.model.RuleEngineResult;
import org.springframework.stereotype.Service;

@Service
public class RiskEvaluationService {
    private static final int MAX_REASON_CODES = 5;

    private final BehaviorScoreCalculator behaviorScoreCalculator;
    private final GraphRiskSignalResolver graphRiskSignalResolver;
    private final RiskWeightProfileService weightProfileService;
    private final RiskScoreCalculator riskScoreCalculator;
    private final RiskReasonGenerator riskReasonGenerator;
    private final RiskScoreResultFactory riskScoreResultFactory;
    private final RiskScoreResultRepository riskScoreResultRepository;

    public RiskEvaluationService(
            BehaviorScoreCalculator behaviorScoreCalculator,
            GraphRiskSignalResolver graphRiskSignalResolver,
            RiskWeightProfileService weightProfileService,
            RiskScoreCalculator riskScoreCalculator,
            RiskReasonGenerator riskReasonGenerator,
            RiskScoreResultFactory riskScoreResultFactory,
            RiskScoreResultRepository riskScoreResultRepository) {
        this.behaviorScoreCalculator = behaviorScoreCalculator;
        this.graphRiskSignalResolver = graphRiskSignalResolver;
        this.weightProfileService = weightProfileService;
        this.riskScoreCalculator = riskScoreCalculator;
        this.riskReasonGenerator = riskReasonGenerator;
        this.riskScoreResultFactory = riskScoreResultFactory;
        this.riskScoreResultRepository = riskScoreResultRepository;
    }

    public RiskScoreResult evaluateAccountRisk(RiskEvaluationRequest request, RuleEngineResult ruleEngineResult) {
        EvaluationArtifacts artifacts = evaluateArtifacts(request, ruleEngineResult);
        RiskScoreResult result = riskScoreResultFactory.build(
                request,
                artifacts.scoreBreakdown(),
                artifacts.profile(),
                artifacts.reasonCodes());
        return riskScoreResultRepository.saveRiskScoreResult(result);
    }

    private EvaluationArtifacts evaluateArtifacts(RiskEvaluationRequest request, RuleEngineResult ruleEngineResult) {
        GraphRiskSignal graphRiskSignal = graphRiskSignalResolver.resolve(request);
        MlAnomalySignal mlAnomalySignal = request.mlAnomalySignal();
        BehaviorRiskSignal behaviorRiskSignal = behaviorScoreCalculator.calculate(request.snapshot(), request.contextSignals());
        RiskWeightProfile profile = weightProfileService.resolveProfile(mlAnomalySignal != null);
        ScoreBreakdown scoreBreakdown = riskScoreCalculator.calculate(
                ruleEngineResult,
                graphRiskSignal,
                mlAnomalySignal,
                behaviorRiskSignal,
                profile);
        List<String> reasonCodes = riskReasonGenerator.generateTopReasonCodes(
                ruleEngineResult,
                graphRiskSignal,
                mlAnomalySignal,
                behaviorRiskSignal,
                MAX_REASON_CODES);
        return new EvaluationArtifacts(profile, scoreBreakdown, reasonCodes);
    }

    public Map<String, RiskScoreResult> evaluateBatchRisk(
            List<RiskEvaluationRequest> requests,
            Map<String, RuleEngineResult> ruleResults) {
        Map<String, RiskScoreResult> results = new LinkedHashMap<>();
        for (RiskEvaluationRequest request : requests) {
            RuleEngineResult ruleResult = ruleResults.get(request.accountId());
            if (ruleResult == null) {
                throw new IllegalArgumentException("Missing RuleEngineResult for accountId " + request.accountId());
            }
            results.put(request.accountId(), evaluateAccountRisk(request, ruleResult));
        }
        return results;
    }

    private record EvaluationArtifacts(
            RiskWeightProfile profile,
            ScoreBreakdown scoreBreakdown,
            List<String> reasonCodes
    ) {
    }
}
