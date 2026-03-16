package me.asu.ta.online.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureQueryService;
import me.asu.ta.online.model.ReevaluateRiskRequest;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskReevaluationService {
    private final FeatureQueryService featureQueryService;
    private final RiskEngineFacade riskEngineFacade;

    public RiskReevaluationService(
            FeatureQueryService featureQueryService,
            RiskEngineFacade riskEngineFacade) {
        this.featureQueryService = featureQueryService;
        this.riskEngineFacade = riskEngineFacade;
    }

    public RiskScoreResult reevaluateAccount(String accountId, ReevaluateRiskRequest request) {
        AccountFeatureSnapshot snapshot = featureQueryService.getLatestFeatures(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No feature snapshot for account " + accountId));
        Map<String, Object> contextSignals = new LinkedHashMap<>();
        if (request != null && request.source() != null && !request.source().isBlank()) {
            contextSignals.put("source", request.source());
        }
        EvaluationMode evaluationMode = request == null ? EvaluationMode.REALTIME : request.evaluationMode();
        return riskEngineFacade.evaluateAccount(new RiskEvaluationRequest(
                accountId,
                snapshot,
                null,
                null,
                contextSignals,
                evaluationMode,
                Instant.now()));
    }
}
