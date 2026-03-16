package me.asu.ta.online.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureQueryService;
import me.asu.ta.online.model.EventRiskEvaluationRequest;
import me.asu.ta.online.model.EventRiskType;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskEventAdapterService {
    private final FeatureQueryService featureQueryService;
    private final RiskEngineFacade riskEngineFacade;

    public RiskEventAdapterService(
            FeatureQueryService featureQueryService,
            RiskEngineFacade riskEngineFacade) {
        this.featureQueryService = featureQueryService;
        this.riskEngineFacade = riskEngineFacade;
    }

    public RiskScoreResult evaluateEvent(EventRiskEvaluationRequest request) {
        if (request == null || request.accountId() == null || request.accountId().isBlank() || request.eventType() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "accountId and eventType are required");
        }
        AccountFeatureSnapshot snapshot = featureQueryService.getLatestFeatures(request.accountId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No feature snapshot for account " + request.accountId()));
        return riskEngineFacade.evaluateAccount(new RiskEvaluationRequest(
                request.accountId(),
                snapshot,
                null,
                null,
                toContextSignals(request),
                EvaluationMode.REALTIME,
                request.eventTime() == null ? Instant.now() : request.eventTime()));
    }

    Map<String, Object> toContextSignals(EventRiskEvaluationRequest request) {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("online.event.type", request.eventType().name());
        if (request.source() != null && !request.source().isBlank()) {
            signals.put("source", request.source());
        }
        if (request.amount() != null) {
            signals.put("online.event.amount", request.amount());
        }
        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            signals.put("online.event.deviceId", request.deviceId());
        }
        signals.putAll(request.attributes());
        if (request.eventType() == EventRiskType.LOGIN || request.eventType() == EventRiskType.NEW_DEVICE_LOGIN) {
            signals.put("online.event.requiresLoginReview", true);
        }
        if (request.eventType() == EventRiskType.WITHDRAW_REQUEST) {
            signals.put("online.event.requiresWithdrawReview", true);
        }
        if (request.eventType() == EventRiskType.LARGE_TRANSFER) {
            signals.put("online.event.requiresTransferReview", true);
        }
        return signals;
    }
}
