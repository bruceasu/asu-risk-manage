package me.asu.ta.online.controller;

import java.util.List;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.online.model.AccountRiskOverview;
import me.asu.ta.online.model.CaseSummaryView;
import me.asu.ta.online.model.FeatureHistoryItem;
import me.asu.ta.online.model.ReevaluateRiskRequest;
import me.asu.ta.online.model.RiskHistoryItem;
import me.asu.ta.online.model.RuleHitSummary;
import me.asu.ta.online.service.AccountRiskQueryService;
import me.asu.ta.online.service.RiskReevaluationService;
import me.asu.ta.risk.model.RiskScoreResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class AccountRiskController {
    private final AccountRiskQueryService accountRiskQueryService;
    private final RiskReevaluationService riskReevaluationService;

    public AccountRiskController(
            AccountRiskQueryService accountRiskQueryService,
            RiskReevaluationService riskReevaluationService) {
        this.accountRiskQueryService = accountRiskQueryService;
        this.riskReevaluationService = riskReevaluationService;
    }

    @GetMapping("/api/accounts/{accountId}/risk/latest")
    public RiskScoreResult getLatestRisk(@PathVariable String accountId) {
        return accountRiskQueryService.getLatestRisk(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No risk result for account " + accountId));
    }

    @GetMapping("/api/accounts/{accountId}/risk/history")
    public List<RiskHistoryItem> getRiskHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return accountRiskQueryService.getRiskHistory(accountId, limit, offset);
    }

    @GetMapping("/api/accounts/{accountId}/risk/overview")
    public AccountRiskOverview getRiskOverview(@PathVariable String accountId) {
        return accountRiskQueryService.getRiskOverview(accountId);
    }

    @GetMapping("/api/accounts/{accountId}/features/latest")
    public AccountFeatureSnapshot getLatestFeature(@PathVariable String accountId) {
        return accountRiskQueryService.getLatestFeature(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No feature snapshot for account " + accountId));
    }

    @GetMapping("/api/accounts/{accountId}/features/history")
    public List<FeatureHistoryItem> getFeatureHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return accountRiskQueryService.getFeatureHistory(accountId, limit, offset);
    }

    @GetMapping("/api/accounts/{accountId}/rules/latest-hits")
    public List<RuleHitSummary> getLatestRuleHits(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        return accountRiskQueryService.getLatestRuleHits(accountId, limit);
    }

    @GetMapping("/api/accounts/{accountId}/cases/latest")
    public CaseSummaryView getLatestCase(@PathVariable String accountId) {
        return accountRiskQueryService.getLatestCase(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No case for account " + accountId));
    }

    @PostMapping("/api/accounts/{accountId}/risk/re-evaluate")
    public RiskScoreResult reevaluateRisk(
            @PathVariable String accountId,
            @RequestBody(required = false) ReevaluateRiskRequest request) {
        return riskReevaluationService.reevaluateAccount(accountId, request);
    }
}
