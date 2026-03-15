package me.asu.ta.risk.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.RuleEngineFacadeContext;
import me.asu.ta.rule.model.RuleEngineResult;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineFacade {
    private final me.asu.ta.rule.service.RuleEngineFacade ruleEngineFacade;
    private final RiskEvaluationService riskEvaluationService;

    public RiskEngineFacade(
            me.asu.ta.rule.service.RuleEngineFacade ruleEngineFacade,
            RiskEvaluationService riskEvaluationService) {
        this.ruleEngineFacade = ruleEngineFacade;
        this.riskEvaluationService = riskEvaluationService;
    }

    public RiskScoreResult evaluateAccount(RiskEvaluationRequest request) {
        RuleEngineResult ruleEngineResult = ruleEngineFacade.evaluateAccount(
                request.accountId(),
                new RuleEngineFacadeContext(
                        request.snapshot(),
                        request.resolvedEvaluationTime(),
                        request.resolvedEvaluationMode(),
                        graphSignals(request.resolvedGraphRiskSignal()),
                        request.contextSignals()));
        return riskEvaluationService.evaluateAccountRisk(request, ruleEngineResult);
    }

    public Map<String, RiskScoreResult> evaluateBatch(List<RiskEvaluationRequest> requests) {
        List<String> accountIds = requests.stream().map(RiskEvaluationRequest::accountId).toList();
        Map<String, RuleEngineFacadeContext> contexts = new LinkedHashMap<>();
        for (RiskEvaluationRequest request : requests) {
            contexts.put(
                    request.accountId(),
                    new RuleEngineFacadeContext(
                            request.snapshot(),
                            request.resolvedEvaluationTime(),
                            request.resolvedEvaluationMode(),
                            graphSignals(request.resolvedGraphRiskSignal()),
                            request.contextSignals()));
        }
        Map<String, RuleEngineResult> ruleResults = ruleEngineFacade.evaluateBatch(accountIds, contexts);
        return riskEvaluationService.evaluateBatchRisk(requests, ruleResults);
    }

    private Map<String, Object> graphSignals(GraphRiskSignal signal) {
        return Map.of(
                "graphScore", signal.graphScore(),
                "graphClusterSize", signal.graphClusterSize(),
                "riskNeighborCount", signal.riskNeighborCount(),
                "sharedDeviceAccounts", signal.sharedDeviceAccounts(),
                "sharedBankAccounts", signal.sharedBankAccounts());
    }
}
