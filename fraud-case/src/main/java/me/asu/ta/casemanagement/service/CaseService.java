package me.asu.ta.casemanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import org.springframework.stereotype.Service;

@Service
public class CaseService {
    private final CaseManagementService caseManagementService;
    private final CaseRetrievalService caseRetrievalService;

    public CaseService(
            CaseManagementService caseManagementService,
            CaseRetrievalService caseRetrievalService) {
        this.caseManagementService = caseManagementService;
        this.caseRetrievalService = caseRetrievalService;
    }

    public InvestigationCaseBundle createCase(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return caseManagementService.createRealtimeCase(accountId);
    }

    public InvestigationCaseBundle createCase(CaseGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        return caseManagementService.createCase(request);
    }

    public Map<String, InvestigationCaseBundle> createBatchCases(List<String> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        return caseManagementService.createBatchCasesForAccounts(accountIds);
    }

    public Map<String, InvestigationCaseBundle> createBatchCasesFromRequests(List<CaseGenerationRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        return caseManagementService.createBatchCases(requests);
    }

    public Optional<InvestigationCaseBundle> getLatestCaseByAccountId(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return caseRetrievalService.getLatestCaseByAccountId(accountId);
    }

    public Optional<InvestigationCaseBundle> getCaseDetailByCaseId(long caseId) {
        return caseRetrievalService.getCaseById(caseId);
    }
}
