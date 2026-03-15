package me.asu.ta.rule.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineFacadeContext;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationContext;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineFacade {
    private final RuleEvaluationService ruleEvaluationService;

    public RuleEngineFacade(RuleEvaluationService ruleEvaluationService) {
        this.ruleEvaluationService = ruleEvaluationService;
    }

    public RuleEngineResult evaluateAccount(String accountId, RuleEngineFacadeContext context) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(context, "context");
        return ruleEvaluationService.evaluate(toEvaluationContext(accountId, context, EvaluationMode.REALTIME));
    }

    public Map<String, RuleEngineResult> evaluateBatch(
            List<String> accountIds,
            Map<String, RuleEngineFacadeContext> contexts) {
        Objects.requireNonNull(accountIds, "accountIds");
        Objects.requireNonNull(contexts, "contexts");
        Map<String, RuleEngineResult> results = new LinkedHashMap<>();
        for (String accountId : accountIds) {
            RuleEngineFacadeContext context = contexts.get(accountId);
            if (context == null) {
                throw new IllegalArgumentException("Missing RuleEngineFacadeContext for accountId " + accountId);
            }
            results.put(accountId, ruleEvaluationService.evaluate(
                    toEvaluationContext(accountId, context, EvaluationMode.BATCH)));
        }
        return results;
    }

    private RuleEvaluationContext toEvaluationContext(
            String accountId,
            RuleEngineFacadeContext facadeContext,
            EvaluationMode defaultMode) {
        AccountFeatureSnapshot snapshot = facadeContext.snapshot();
        if (!accountId.equals(snapshot.accountId())) {
            throw new IllegalArgumentException("Account id mismatch between request and snapshot for " + accountId);
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("snapshot", snapshot);
        attributes.put("graphSignals", facadeContext.graphSignals());
        attributes.putAll(facadeContext.graphSignals());
        attributes.putAll(facadeContext.attributes());
        return new RuleEvaluationContext(
                accountId,
                facadeContext.resolvedEvaluationTime(),
                snapshot.featureVersion(),
                facadeContext.resolvedEvaluationMode(defaultMode),
                Map.of(),
                attributes);
    }
}
