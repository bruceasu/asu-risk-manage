package me.asu.ta.online.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseFacade;
import me.asu.ta.feature.model.AccountFeatureHistory;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.repository.AccountFeatureHistoryRepository;
import me.asu.ta.feature.repository.AccountFeatureSnapshotRepository;
import me.asu.ta.online.model.AccountRiskOverview;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.risk.repository.RiskScoreResultRepository;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleHitLog;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import org.junit.jupiter.api.Test;

class AccountRiskQueryServiceTest {
    @Test
    void shouldAssembleOverviewWithPartialData() {
        AccountFeatureSnapshot snapshot = AccountFeatureSnapshot.builder("acct-q", Instant.now())
                .featureVersion(1)
                .transactionCount24h(3)
                .build();
        RiskScoreResult risk = new RiskScoreResult(
                1L, "acct-q", 88.0d, RiskLevel.HIGH, "DEFAULT", 1, Instant.now(),
                EvaluationMode.REALTIME, List.of("RULE_A"), new ScoreBreakdown(80, 70, 0, 60, 88, "DEFAULT"));
        RiskScoreResultRepository riskRepository = new RiskScoreResultRepository(null, new ObjectMapper()) {
            @Override
            public Optional<RiskScoreResult> findLatestRiskScoreByAccountId(String accountId) {
                return Optional.of(risk);
            }
        };
        AccountFeatureSnapshotRepository snapshotRepository = new AccountFeatureSnapshotRepository(null) {
            @Override
            public Optional<AccountFeatureSnapshot> findLatestByAccountId(String accountId) {
                return Optional.of(snapshot);
            }
        };
        AccountFeatureHistoryRepository historyRepository = new AccountFeatureHistoryRepository(null);
        RuleHitLogRepository ruleHitLogRepository = new RuleHitLogRepository(null) {
            @Override
            public List<RuleHitLog> findLatestByAccountId(String accountId, int limit) {
                return List.of(new RuleHitLog(
                        1L, "acct-q", "RULE_A", 1, Instant.now(), 30, "RULE_A", "{}", 1, EvaluationMode.REALTIME));
            }
        };
        CaseFacade caseFacade = new CaseFacade(null) {
            @Override
            public Optional<InvestigationCaseBundle> getLatestCaseByAccountId(String accountId) {
                return Optional.empty();
            }
        };

        AccountRiskQueryService service = new AccountRiskQueryService(
                riskRepository, snapshotRepository, historyRepository, ruleHitLogRepository, caseFacade);
        AccountRiskOverview overview = service.getRiskOverview("acct-q");

        assertEquals("acct-q", overview.accountId());
        assertNotNull(overview.latestRisk());
        assertEquals(1, overview.latestRuleHits().size());
        assertEquals(List.of("RULE_A"), overview.topReasonCodes());
    }

    @Test
    void shouldMapFeatureHistory() {
        RiskScoreResultRepository riskRepository = new RiskScoreResultRepository(null, new ObjectMapper());
        AccountFeatureSnapshotRepository snapshotRepository = new AccountFeatureSnapshotRepository(null);
        AccountFeatureHistory history = new AccountFeatureHistory(
                9L, Instant.now(), "acct-h", 3,
                null, null, null, null, null, null, null, null, null, null, null,
                6, 1200.0d, 200.0d, null, null, null, null, null, null, null, null, null, null, null, null,
                4, null, null, null, null, null, 7, 2);
        AccountFeatureHistoryRepository historyRepository = new AccountFeatureHistoryRepository(null) {
            @Override
            public List<AccountFeatureHistory> findByAccountId(String accountId, int limit, int offset) {
                return List.of(history);
            }
        };
        RuleHitLogRepository ruleHitLogRepository = new RuleHitLogRepository(null);
        CaseFacade caseFacade = new CaseFacade(null);

        AccountRiskQueryService service = new AccountRiskQueryService(
                riskRepository, snapshotRepository, historyRepository, ruleHitLogRepository, caseFacade);

        assertEquals(1, service.getFeatureHistory("acct-h", 20, 0).size());
        assertEquals(4, service.getFeatureHistory("acct-h", 20, 0).getFirst().sharedDeviceAccounts7d());
    }
}
