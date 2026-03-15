package me.asu.ta.casemanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.repository.CaseFeatureSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseGraphSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRecommendedActionRepository;
import me.asu.ta.casemanagement.repository.CaseRiskSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRuleHitRepository;
import me.asu.ta.casemanagement.repository.CaseTimelineEventRepository;
import me.asu.ta.casemanagement.repository.InvestigationCaseRepository;
import org.springframework.stereotype.Service;

@Service
public class CaseRetrievalService {
    private final InvestigationCaseRepository investigationCaseRepository;
    private final CaseRiskSummaryRepository riskSummaryRepository;
    private final CaseFeatureSummaryRepository featureSummaryRepository;
    private final CaseRuleHitRepository ruleHitRepository;
    private final CaseGraphSummaryRepository graphSummaryRepository;
    private final CaseTimelineEventRepository timelineEventRepository;
    private final CaseRecommendedActionRepository recommendedActionRepository;

    public CaseRetrievalService(
            InvestigationCaseRepository investigationCaseRepository,
            CaseRiskSummaryRepository riskSummaryRepository,
            CaseFeatureSummaryRepository featureSummaryRepository,
            CaseRuleHitRepository ruleHitRepository,
            CaseGraphSummaryRepository graphSummaryRepository,
            CaseTimelineEventRepository timelineEventRepository,
            CaseRecommendedActionRepository recommendedActionRepository) {
        this.investigationCaseRepository = investigationCaseRepository;
        this.riskSummaryRepository = riskSummaryRepository;
        this.featureSummaryRepository = featureSummaryRepository;
        this.ruleHitRepository = ruleHitRepository;
        this.graphSummaryRepository = graphSummaryRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.recommendedActionRepository = recommendedActionRepository;
    }

    public Optional<InvestigationCaseBundle> getCaseById(long caseId) {
        return investigationCaseRepository.findByCaseId(caseId).map(this::assemble);
    }

    public Optional<InvestigationCaseBundle> getLatestCaseByAccountId(String accountId) {
        return investigationCaseRepository.findLatestByAccountId(accountId).map(this::assemble);
    }

    public Map<String, InvestigationCaseBundle> getLatestCasesByAccountIds(List<String> accountIds) {
        Map<String, InvestigationCaseBundle> results = new LinkedHashMap<>();
        for (InvestigationCase investigationCase : investigationCaseRepository.findBatchByAccountIds(accountIds)) {
            results.putIfAbsent(investigationCase.accountId(), assemble(investigationCase));
        }
        return results;
    }

    private InvestigationCaseBundle assemble(InvestigationCase investigationCase) {
        long caseId = investigationCase.caseId();
        return new InvestigationCaseBundle(
                investigationCase,
                riskSummaryRepository.findByCaseId(caseId).orElseThrow(),
                featureSummaryRepository.findByCaseId(caseId).orElseThrow(),
                ruleHitRepository.findByCaseId(caseId),
                graphSummaryRepository.findByCaseId(caseId).orElseThrow(),
                timelineEventRepository.findByCaseId(caseId),
                recommendedActionRepository.findByCaseId(caseId));
    }
}
