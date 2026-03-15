package me.asu.ta.risk.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.classification.RiskLevelClassifier;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskLevel;
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
    private final RiskWeightProfileService weightProfileService;
    private final RiskScoreCalculator riskScoreCalculator;
    private final RiskLevelClassifier riskLevelClassifier;
    private final RiskReasonGenerator riskReasonGenerator;
    private final RiskScoreResultRepository riskScoreResultRepository;

    public RiskEvaluationService(
            BehaviorScoreCalculator behaviorScoreCalculator,
            RiskWeightProfileService weightProfileService,
            RiskScoreCalculator riskScoreCalculator,
            RiskLevelClassifier riskLevelClassifier,
            RiskReasonGenerator riskReasonGenerator,
            RiskScoreResultRepository riskScoreResultRepository) {
        this.behaviorScoreCalculator = behaviorScoreCalculator;
        this.weightProfileService = weightProfileService;
        this.riskScoreCalculator = riskScoreCalculator;
        this.riskLevelClassifier = riskLevelClassifier;
        this.riskReasonGenerator = riskReasonGenerator;
        this.riskScoreResultRepository = riskScoreResultRepository;
    }

    public RiskScoreResult evaluateAccountRisk(RiskEvaluationRequest request, RuleEngineResult ruleEngineResult) {
        GraphRiskSignal graphRiskSignal = resolveGraphRiskSignal(request);
        MlAnomalySignal mlAnomalySignal = resolveMlAnomalySignal(request.snapshot(), request.mlAnomalySignal(), request.resolvedEvaluationTime());
        BehaviorRiskSignal behaviorRiskSignal = behaviorScoreCalculator.calculate(request.snapshot());
        RiskWeightProfile profile = weightProfileService.resolveProfile(mlAnomalySignal != null);
        ScoreBreakdown scoreBreakdown = riskScoreCalculator.calculate(
                ruleEngineResult,
                graphRiskSignal,
                mlAnomalySignal,
                behaviorRiskSignal,
                profile);
        RiskLevel riskLevel = riskLevelClassifier.classify(scoreBreakdown.finalScore());
        List<String> reasonCodes = riskReasonGenerator.generateTopReasonCodes(
                ruleEngineResult,
                graphRiskSignal,
                mlAnomalySignal,
                behaviorRiskSignal,
                MAX_REASON_CODES);
        RiskScoreResult result = new RiskScoreResult(
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
        return riskScoreResultRepository.saveRiskScoreResult(result);
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

    private GraphRiskSignal resolveGraphRiskSignal(RiskEvaluationRequest request) {
        if (request.graphRiskSignal() != null) {
            return request.graphRiskSignal();
        }
        AccountFeatureSnapshot snapshot = request.snapshot();
        double score = 0.0d;
        if (intValue(snapshot.riskNeighborCount30d()) >= 3) {
            score += 40.0d;
        }
        if (intValue(snapshot.graphClusterSize30d()) >= 5) {
            score += 30.0d;
        }
        if (intValue(snapshot.sharedDeviceAccounts7d()) >= 5) {
            score += 15.0d;
        }
        if (intValue(snapshot.sharedBankAccounts30d()) >= 3) {
            score += 15.0d;
        }
        return new GraphRiskSignal(
                Math.min(score, 100.0d),
                intValue(snapshot.graphClusterSize30d()),
                intValue(snapshot.riskNeighborCount30d()),
                intValue(snapshot.sharedDeviceAccounts7d()),
                intValue(snapshot.sharedBankAccounts30d()));
    }

    private MlAnomalySignal resolveMlAnomalySignal(AccountFeatureSnapshot snapshot, MlAnomalySignal signal, Instant evaluationTime) {
        if (signal != null) {
            return signal;
        }
        if (snapshot.anomalyScoreLast() == null) {
            return null;
        }
        double raw = snapshot.anomalyScoreLast();
        double normalized = raw <= 1.0d ? raw * 100.0d : Math.min(raw, 100.0d);
        return new MlAnomalySignal(raw, normalized, "feature_snapshot_anomaly", evaluationTime);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}
