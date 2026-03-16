package me.asu.ta.risk.reason;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.risk.model.BehaviorRiskSignal;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.MlAnomalySignal;
import me.asu.ta.risk.model.RiskReasonMapping;
import me.asu.ta.risk.repository.RiskReasonMappingRepository;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import org.springframework.stereotype.Component;

@Component
public class RiskReasonGenerator {
    private final RiskReasonMappingRepository mappingRepository;

    public RiskReasonGenerator(RiskReasonMappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    public List<String> generateTopReasonCodes(
            RuleEngineResult ruleEngineResult,
            GraphRiskSignal graphRiskSignal,
            MlAnomalySignal mlAnomalySignal,
            BehaviorRiskSignal behaviorRiskSignal,
            int limit) {
        Map<String, ReasonCandidate> ranked = new LinkedHashMap<>();
        collectRuleReasonCandidates(ranked, ruleEngineResult);
        collectGraphReasonCandidates(ranked, graphRiskSignal);
        collectMlReasonCandidates(ranked, mlAnomalySignal);
        collectBehaviorReasonCandidates(ranked, behaviorRiskSignal);
        return ranked.values().stream()
                .sorted((left, right) -> Integer.compare(right.rank(), left.rank()))
                .limit(Math.max(1, limit))
                .map(ReasonCandidate::reasonCode)
                .toList();
    }

    private void collectRuleReasonCandidates(Map<String, ReasonCandidate> ranked, RuleEngineResult ruleEngineResult) {
        if (ruleEngineResult == null) {
            return;
        }
        for (RuleEvaluationResult hit : ruleEngineResult.hits()) {
            merge(ranked, new ReasonCandidate(hit.reasonCode(), hit.severity(), 400));
        }
    }

    private void collectGraphReasonCandidates(Map<String, ReasonCandidate> ranked, GraphRiskSignal graphRiskSignal) {
        if (graphRiskSignal == null) {
            return;
        }
        if (graphRiskSignal.riskNeighborCount() >= 3) {
            merge(ranked, candidate("GRAPH_HIGH_RISK_NEIGHBORS", RuleSeverity.HIGH, 320));
        }
        if (graphRiskSignal.sharedDeviceAccounts() >= 5) {
            merge(ranked, candidate("GRAPH_SHARED_DEVICE_CLUSTER", RuleSeverity.HIGH, 310));
        }
        if (graphRiskSignal.sharedBankAccounts() >= 3) {
            merge(ranked, candidate("GRAPH_SHARED_BANK_CLUSTER", RuleSeverity.HIGH, 300));
        }
        if (graphRiskSignal.graphClusterSize() >= 5) {
            merge(ranked, candidate("GRAPH_LARGE_CLUSTER", RuleSeverity.MEDIUM, 260));
        }
    }

    private void collectMlReasonCandidates(Map<String, ReasonCandidate> ranked, MlAnomalySignal mlAnomalySignal) {
        if (mlAnomalySignal == null) {
            return;
        }
        if (mlAnomalySignal.anomalyScoreNormalized() >= 80.0d) {
            merge(ranked, candidate("ML_ANOMALY_HIGH", RuleSeverity.HIGH, 250));
            return;
        }
        if (mlAnomalySignal.anomalyScoreNormalized() >= 60.0d) {
            merge(ranked, candidate("ML_ANOMALY_MEDIUM", RuleSeverity.MEDIUM, 210));
        }
    }

    private void collectBehaviorReasonCandidates(
            Map<String, ReasonCandidate> ranked,
            BehaviorRiskSignal behaviorRiskSignal) {
        if (behaviorRiskSignal == null) {
            return;
        }
        for (String reasonCode : behaviorRiskSignal.reasonCodes()) {
            merge(ranked, candidate(reasonCode, RuleSeverity.MEDIUM, 220));
        }
    }

    private ReasonCandidate candidate(String reasonCode, RuleSeverity fallbackSeverity, int sourcePriority) {
        RuleSeverity severity = mappingRepository.findReasonMapping(reasonCode)
                .map(RiskReasonMapping::severity)
                .orElse(fallbackSeverity);
        return new ReasonCandidate(reasonCode, severity, severityRank(severity) + sourcePriority);
    }

    private void merge(Map<String, ReasonCandidate> ranked, ReasonCandidate incoming) {
        if (incoming.reasonCode() == null || incoming.reasonCode().isBlank()) {
            return;
        }
        ranked.merge(
                incoming.reasonCode(),
                incoming,
                (current, candidate) -> current.rank() >= candidate.rank() ? current : candidate);
    }

    private int severityRank(RuleSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 400;
            case HIGH -> 300;
            case MEDIUM -> 200;
            case LOW -> 100;
        };
    }

    private record ReasonCandidate(String reasonCode, RuleSeverity severity, int rank) {
    }
}
