package me.asu.ta.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import me.asu.ta.rule.engine.RuleEngine;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationContext;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleHitLog;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import org.springframework.stereotype.Service;

@Service
public class RuleEvaluationService {
    private final RuleConfigService ruleConfigService;
    private final RuleEngine ruleEngine;
    private final RuleHitLogRepository ruleHitLogRepository;
    private final ObjectMapper objectMapper;

    public RuleEvaluationService(
            RuleConfigService ruleConfigService,
            RuleEngine ruleEngine,
            RuleHitLogRepository ruleHitLogRepository,
            ObjectMapper objectMapper) {
        this.ruleConfigService = ruleConfigService;
        this.ruleEngine = ruleEngine;
        this.ruleHitLogRepository = ruleHitLogRepository;
        this.objectMapper = objectMapper;
    }

    public RuleEngineResult evaluate(RuleEvaluationContext context) {
        Map<String, RuleConfig> activeConfigs = ruleConfigService.getActiveConfigs(context.evaluationTime());
        RuleEngineResult result = ruleEngine.evaluate(context, activeConfigs);
        persistHits(context, result);
        return result;
    }

    private void persistHits(RuleEvaluationContext context, RuleEngineResult result) {
        for (RuleEvaluationResult hit : result.hits()) {
            ruleHitLogRepository.insertRuleHit(new RuleHitLog(
                    0L,
                    context.accountId(),
                    hit.ruleCode(),
                    hit.ruleVersion(),
                    Instant.now(),
                    hit.score(),
                    hit.reasonCode(),
                    toJson(hit.evidence()),
                    context.featureVersion(),
                    context.evaluationMode()));
        }
    }

    private String toJson(Map<String, Object> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize rule evidence", ex);
        }
    }
}
