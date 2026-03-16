package me.asu.ta.online.service;

import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseFacade;
import me.asu.ta.feature.model.AccountFeatureHistory;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureHistoryRepository;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import me.asu.ta.online.model.AccountRiskOverview;
import me.asu.ta.online.model.CaseSummaryView;
import me.asu.ta.online.model.FeatureHistoryItem;
import me.asu.ta.online.model.RiskHistoryItem;
import me.asu.ta.online.model.RuleHitSummary;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.repository.RiskScoreResultRepository;
import me.asu.ta.rule.model.RuleHitLog;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountRiskQueryService {
    private static final int DEFAULT_RULE_HIT_LIMIT = 10;

    private final RiskScoreResultRepository riskScoreResultRepository;
    private final AccountFeatureSnapshotRepository snapshotRepository;
    private final AccountFeatureHistoryRepository historyRepository;
    private final RuleHitLogRepository ruleHitLogRepository;
    private final CaseFacade caseFacade;

    public AccountRiskQueryService(
            RiskScoreResultRepository riskScoreResultRepository,
            AccountFeatureSnapshotRepository snapshotRepository,
            AccountFeatureHistoryRepository historyRepository,
            RuleHitLogRepository ruleHitLogRepository,
            CaseFacade caseFacade) {
        this.riskScoreResultRepository = riskScoreResultRepository;
        this.snapshotRepository = snapshotRepository;
        this.historyRepository = historyRepository;
        this.ruleHitLogRepository = ruleHitLogRepository;
        this.caseFacade = caseFacade;
    }

    public Optional<RiskScoreResult> getLatestRisk(String accountId) {
        return riskScoreResultRepository.findLatestRiskScoreByAccountId(accountId);
    }

    public List<RiskHistoryItem> getRiskHistory(String accountId, int limit, int offset) {
        return riskScoreResultRepository.findHistoryByAccountId(accountId, limit, offset).stream()
                .map(this::toRiskHistoryItem)
                .toList();
    }

    public Optional<AccountFeatureSnapshot> getLatestFeature(String accountId) {
        return snapshotRepository.findLatestByAccountId(accountId);
    }

    public List<FeatureHistoryItem> getFeatureHistory(String accountId, int limit, int offset) {
        return historyRepository.findByAccountId(accountId, limit, offset).stream()
                .map(this::toFeatureHistoryItem)
                .toList();
    }

    public List<RuleHitSummary> getLatestRuleHits(String accountId, int limit) {
        return ruleHitLogRepository.findLatestByAccountId(accountId, limit).stream()
                .map(this::toRuleHitSummary)
                .toList();
    }

    public Optional<CaseSummaryView> getLatestCase(String accountId) {
        return caseFacade.getLatestCaseByAccountId(accountId).map(this::toCaseSummaryView);
    }

    public AccountRiskOverview getRiskOverview(String accountId) {
        Optional<RiskScoreResult> risk = getLatestRisk(accountId);
        Optional<AccountFeatureSnapshot> snapshot = getLatestFeature(accountId);
        List<RuleHitSummary> hits = getLatestRuleHits(accountId, DEFAULT_RULE_HIT_LIMIT);
        Optional<CaseSummaryView> latestCase = getLatestCase(accountId);
        return new AccountRiskOverview(
                accountId,
                risk.map(this::toRiskHistoryItem).orElse(null),
                risk.map(RiskScoreResult::topReasonCodes).orElse(List.of()),
                risk.map(RiskScoreResult::scoreBreakdown).orElse(null),
                snapshot.orElse(null),
                hits,
                latestCase.orElse(null));
    }

    private RiskHistoryItem toRiskHistoryItem(RiskScoreResult result) {
        return new RiskHistoryItem(
                result.scoreId(),
                result.riskScore(),
                result.riskLevel(),
                result.profileName(),
                result.featureVersion(),
                result.generatedAt(),
                result.evaluationMode(),
                result.topReasonCodes(),
                result.scoreBreakdown());
    }

    private FeatureHistoryItem toFeatureHistoryItem(AccountFeatureHistory history) {
        return new FeatureHistoryItem(
                history.snapshotId(),
                history.snapshotTime(),
                history.featureVersion(),
                history.transactionCount24h(),
                history.totalAmount24h(),
                history.sharedDeviceAccounts7d(),
                history.graphClusterSize30d(),
                history.riskNeighborCount30d());
    }

    private RuleHitSummary toRuleHitSummary(RuleHitLog hitLog) {
        return new RuleHitSummary(
                hitLog.hitId(),
                hitLog.ruleCode(),
                hitLog.ruleVersion(),
                hitLog.hitTime(),
                hitLog.score(),
                hitLog.reasonCode(),
                hitLog.evidenceJson(),
                hitLog.featureVersion(),
                hitLog.evaluationMode());
    }

    private CaseSummaryView toCaseSummaryView(InvestigationCaseBundle bundle) {
        return new CaseSummaryView(
                bundle.investigationCase().caseId(),
                bundle.investigationCase().caseStatus(),
                bundle.investigationCase().riskScore(),
                bundle.investigationCase().riskLevel(),
                bundle.investigationCase().profileName(),
                bundle.investigationCase().topReasonCodes(),
                bundle.investigationCase().featureVersion(),
                bundle.investigationCase().evaluationMode(),
                bundle.investigationCase().createdAt(),
                bundle.investigationCase().updatedAt());
    }
}
