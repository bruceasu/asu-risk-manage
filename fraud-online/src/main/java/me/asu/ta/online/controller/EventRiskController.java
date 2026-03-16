package me.asu.ta.online.controller;

import me.asu.ta.online.model.EventRiskEvaluationRequest;
import me.asu.ta.online.service.RiskEventAdapterService;
import me.asu.ta.risk.model.RiskScoreResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventRiskController {
    private final RiskEventAdapterService riskEventAdapterService;

    public EventRiskController(RiskEventAdapterService riskEventAdapterService) {
        this.riskEventAdapterService = riskEventAdapterService;
    }

    @PostMapping("/api/events/risk-evaluate")
    public RiskScoreResult evaluateEvent(@RequestBody EventRiskEvaluationRequest request) {
        return riskEventAdapterService.evaluateEvent(request);
    }
}
