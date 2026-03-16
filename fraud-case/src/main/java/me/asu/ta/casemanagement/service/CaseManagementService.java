package me.asu.ta.casemanagement.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.casemanagement.builder.InvestigationCaseBuilder;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.recommendation.CaseRecommendationBuilder;
import me.asu.ta.casemanagement.repository.CaseFeatureSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseGraphSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRecommendedActionRepository;
import me.asu.ta.casemanagement.repository.CaseRiskSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRuleHitRepository;
import me.asu.ta.casemanagement.repository.CaseTimelineEventRepository;
import me.asu.ta.casemanagement.repository.InvestigationCaseRepository;
import me.asu.ta.casemanagement.timeline.CaseTimelineBuilder;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureStoreService;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.service.RiskEvaluationService;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineFacadeContext;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.service.RuleEngineFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseManagementService {
    private final FeatureStoreService featureStoreService;
    private final RuleEngineFacade ruleEngineFacade;
    private final RiskEvaluationService riskEvaluationService;
    private final InvestigationCaseBuilder caseBuilder;
    private final CaseTimelineBuilder caseTimelineBuilder;
    private final CaseRecommendationBuilder recommendationBuilder;
    private final InvestigationCaseRepository investigationCaseRepository;
    private final CaseRiskSummaryRepository riskSummaryRepository;
    private final CaseFeatureSummaryRepository featureSummaryRepository;
    private final CaseRuleHitRepository ruleHitRepository;
    private final CaseGraphSummaryRepository graphSummaryRepository;
    private final CaseTimelineEventRepository timelineEventRepository;
    private final CaseRecommendedActionRepository recommendedActionRepository;
    private final CaseRetrievalService caseRetrievalService;

    public CaseManagementService(
            FeatureStoreService featureStoreService,
            RuleEngineFacade ruleEngineFacade,
            RiskEvaluationService riskEvaluationService,
            InvestigationCaseBuilder caseBuilder,
            CaseTimelineBuilder caseTimelineBuilder,
            CaseRecommendationBuilder recommendationBuilder,
            InvestigationCaseRepository investigationCaseRepository,
            CaseRiskSummaryRepository riskSummaryRepository,
            CaseFeatureSummaryRepository featureSummaryRepository,
            CaseRuleHitRepository ruleHitRepository,
            CaseGraphSummaryRepository graphSummaryRepository,
            CaseTimelineEventRepository timelineEventRepository,
            CaseRecommendedActionRepository recommendedActionRepository,
            CaseRetrievalService caseRetrievalService) {
        this.featureStoreService = featureStoreService;
        this.ruleEngineFacade = ruleEngineFacade;
        this.riskEvaluationService = riskEvaluationService;
        this.caseBuilder = caseBuilder;
        this.caseTimelineBuilder = caseTimelineBuilder;
        this.recommendationBuilder = recommendationBuilder;
        this.investigationCaseRepository = investigationCaseRepository;
        this.riskSummaryRepository = riskSummaryRepository;
        this.featureSummaryRepository = featureSummaryRepository;
        this.ruleHitRepository = ruleHitRepository;
        this.graphSummaryRepository = graphSummaryRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.recommendedActionRepository = recommendedActionRepository;
        this.caseRetrievalService = caseRetrievalService;
    }

    public InvestigationCaseBundle createRealtimeCase(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        AccountFeatureSnapshot snapshot = featureStoreService.getLatestFeatures(accountId)
                .orElseThrow(() -> new IllegalArgumentException("No latest feature snapshot for accountId " + accountId));
        return createCase(new CaseGenerationRequest(
                accountId,
                snapshot,
                null,
                null,
                Map.of(),
                EvaluationMode.REALTIME,
                Instant.now()));
    }

    @Transactional
    public InvestigationCaseBundle createCase(CaseGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        RuleEngineResult ruleEngineResult = ruleEngineFacade.evaluateAccount(
                request.accountId(),
                new RuleEngineFacadeContext(
                        request.snapshot(),
                        request.resolvedEvaluationTime(),
                        request.resolvedEvaluationMode(),
                        graphSignalMap(resolveGraphSignal(request.snapshot(), request.graphRiskSignal())),
                        request.contextSignals()));
        RiskScoreResult riskScoreResult = riskEvaluationService.evaluateAccountRisk(toRiskRequest(request), ruleEngineResult);
        return persistCaseArtifacts(request, ruleEngineResult, riskScoreResult);
    }

    public Map<String, InvestigationCaseBundle> createBatchCasesForAccounts(List<String> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        Map<String, AccountFeatureSnapshot> snapshots = featureStoreService.getLatestFeaturesBatch(accountIds);
        List<CaseGenerationRequest> requests = new ArrayList<>();
        for (String accountId : accountIds) {
            AccountFeatureSnapshot snapshot = snapshots.get(accountId);
            if (snapshot != null) {
                requests.add(new CaseGenerationRequest(
                        accountId,
                        snapshot,
                        null,
                        null,
                        Map.of(),
                        EvaluationMode.BATCH,
                        Instant.now()));
            }
        }
        return createBatchCases(requests);
    }

    @Transactional
    public Map<String, InvestigationCaseBundle> createBatchCases(List<CaseGenerationRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        Map<String, InvestigationCaseBundle> bundles = new LinkedHashMap<>();
        if (requests.isEmpty()) {
            return bundles;
        }

        List<String> accountIds = requests.stream().map(CaseGenerationRequest::accountId).toList();
        Map<String, RuleEngineFacadeContext> contexts = new LinkedHashMap<>();
        for (CaseGenerationRequest request : requests) {
            contexts.put(request.accountId(), new RuleEngineFacadeContext(
                    request.snapshot(),
                    request.resolvedEvaluationTime(),
                    request.resolvedEvaluationMode(),
                    graphSignalMap(resolveGraphSignal(request.snapshot(), request.graphRiskSignal())),
                    request.contextSignals()));
        }

        Map<String, RuleEngineResult> ruleResults = ruleEngineFacade.evaluateBatch(accountIds, contexts);
        List<RiskEvaluationRequest> riskRequests = requests.stream().map(this::toRiskRequest).toList();
        Map<String, RiskScoreResult> riskResults = riskEvaluationService.evaluateBatchRisk(riskRequests, ruleResults);

        for (CaseGenerationRequest request : requests) {
            bundles.put(
                    request.accountId(),
                    persistCaseArtifacts(request, ruleResults.get(request.accountId()), riskResults.get(request.accountId())));
        }
        return bundles;
    }

    private InvestigationCaseBundle persistCaseArtifacts(
            CaseGenerationRequest request,
            RuleEngineResult ruleEngineResult,
            RiskScoreResult riskScoreResult) {
        InvestigationCase savedCase = investigationCaseRepository.save(
                caseBuilder.buildCaseHeader(request.snapshot(), riskScoreResult));
        long caseId = savedCase.caseId();

        CaseRiskSummary riskSummary = caseBuilder.buildRiskSummary(caseId, riskScoreResult);
        CaseGraphSummary graphSummary = caseBuilder.buildGraphSummary(caseId, request, riskScoreResult.generatedAt());
        List<CaseRuleHit> ruleHits = caseBuilder.buildRuleHits(caseId, ruleEngineResult, riskScoreResult.generatedAt());
        List<CaseTimelineEvent> timelineEvents = caseTimelineBuilder.build(
                caseId,
                request.snapshot(),
                ruleEngineResult,
                riskScoreResult,
                request.contextSignals());
        List<CaseRecommendedAction> recommendedActions =
                recommendationBuilder.build(caseId, request.snapshot(), ruleEngineResult, riskScoreResult, request.contextSignals());

        riskSummaryRepository.save(riskSummary);
        featureSummaryRepository.save(
                caseBuilder.buildFeatureSummary(caseId, request.snapshot(), request.mlAnomalySignal(), riskScoreResult.generatedAt()));
        graphSummaryRepository.save(graphSummary);
        ruleHitRepository.insertBatch(ruleHits);
        timelineEventRepository.insertBatch(timelineEvents);
        recommendedActionRepository.insertBatch(recommendedActions);

        return caseRetrievalService.getCaseById(caseId).orElseThrow();
    }

    private RiskEvaluationRequest toRiskRequest(CaseGenerationRequest request) {
        return new RiskEvaluationRequest(
                request.accountId(),
                request.snapshot(),
                request.graphRiskSignal(),
                request.mlAnomalySignal(),
                request.contextSignals(),
                request.resolvedEvaluationMode(),
                request.resolvedEvaluationTime());
    }

    private GraphRiskSignal resolveGraphSignal(AccountFeatureSnapshot snapshot, GraphRiskSignal signal) {
        if (signal != null) {
            return signal;
        }
        double score = 0.0d;
        if (intValue(snapshot.riskNeighborCount30d()) >= 3) {
            score += 40.0d;
        }
        if (intValue(snapshot.graphClusterSize30d()) >= 5) {
            score += 30.0d;
        }
        if (intValue(snapshot.sharedDeviceAccounts7d()) >= 5) {
            score += 15.0d;
        }
        if (intValue(snapshot.sharedBankAccounts30d()) >= 3) {
            score += 15.0d;
        }
        return new GraphRiskSignal(
                Math.min(score, 100.0d),
                intValue(snapshot.graphClusterSize30d()),
                intValue(snapshot.riskNeighborCount30d()),
                intValue(snapshot.sharedDeviceAccounts7d()),
                intValue(snapshot.sharedBankAccounts30d()));
    }

    private Map<String, Object> graphSignalMap(GraphRiskSignal signal) {
        return Map.of(
                "graphScore", signal.graphScore(),
                "graphClusterSize", signal.graphClusterSize(),
                "riskNeighborCount", signal.riskNeighborCount(),
                "sharedDeviceAccounts", signal.sharedDeviceAccounts(),
                "sharedBankAccounts", signal.sharedBankAccounts());
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}
