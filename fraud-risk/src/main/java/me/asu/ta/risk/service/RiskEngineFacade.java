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
    private final GraphRiskSignalResolver graphRiskSignalResolver;

    public RiskEngineFacade(
            me.asu.ta.rule.service.RuleEngineFacade ruleEngineFacade,
            RiskEvaluationService riskEvaluationService,
            GraphRiskSignalResolver graphRiskSignalResolver) {
        this.ruleEngineFacade = ruleEngineFacade;
        this.riskEvaluationService = riskEvaluationService;
        this.graphRiskSignalResolver = graphRiskSignalResolver;
    }

    public RiskScoreResult evaluateAccount(RiskEvaluationRequest request) {
        GraphRiskSignal graphRiskSignal = graphRiskSignalResolver.resolve(request);
        RuleEngineResult ruleEngineResult = ruleEngineFacade.evaluateAccount(
                request.accountId(),
                new RuleEngineFacadeContext(
                        request.snapshot(),
                        request.resolvedEvaluationTime(),
                        request.resolvedEvaluationMode(),
                        graphRiskSignalResolver.toContextMap(graphRiskSignal),
                        request.contextSignals()));
        return riskEvaluationService.evaluateAccountRisk(request, ruleEngineResult);
    }

    public Map<String, RiskScoreResult> evaluateBatch(List<RiskEvaluationRequest> requests) {
        List<String> accountIds = requests.stream().map(RiskEvaluationRequest::accountId).toList();
        Map<String, RuleEngineFacadeContext> contexts = new LinkedHashMap<>();
        for (RiskEvaluationRequest request : requests) {
            GraphRiskSignal graphRiskSignal = graphRiskSignalResolver.resolve(request);
            contexts.put(
                    request.accountId(),
                    new RuleEngineFacadeContext(
                            request.snapshot(),
                            request.resolvedEvaluationTime(),
                            request.resolvedEvaluationMode(),
                            graphRiskSignalResolver.toContextMap(graphRiskSignal),
                            request.contextSignals()));
        }
        Map<String, RuleEngineResult> ruleResults = ruleEngineFacade.evaluateBatch(accountIds, contexts);
        return riskEvaluationService.evaluateBatchRisk(requests, ruleResults);
    }
}
