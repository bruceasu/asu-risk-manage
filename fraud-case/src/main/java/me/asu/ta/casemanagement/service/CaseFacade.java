package me.asu.ta.casemanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import org.springframework.stereotype.Service;

@Service
public class CaseFacade {
    private final CaseService caseService;

    public CaseFacade(CaseService caseService) {
        this.caseService = caseService;
    }

    public InvestigationCaseBundle createCase(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return caseService.createCase(accountId);
    }

    public InvestigationCaseBundle createCase(CaseGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        return caseService.createCase(request);
    }

    public Map<String, InvestigationCaseBundle> createBatchCases(List<String> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        return caseService.createBatchCases(accountIds);
    }

    public Map<String, InvestigationCaseBundle> createBatchCasesFromRequests(List<CaseGenerationRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        Map<String, InvestigationCaseBundle> results = new LinkedHashMap<>();
        results.putAll(caseService.createBatchCasesFromRequests(requests));
        return results;
    }

    public Optional<InvestigationCaseBundle> getLatestCaseByAccountId(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return caseService.getLatestCaseByAccountId(accountId);
    }

    public Optional<InvestigationCaseBundle> getCaseDetailByCaseId(long caseId) {
        return caseService.getCaseDetailByCaseId(caseId);
    }
}
