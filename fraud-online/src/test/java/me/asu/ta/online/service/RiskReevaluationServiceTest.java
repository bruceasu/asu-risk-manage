package me.asu.ta.online.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.feature.service.FeatureQueryService;
import me.asu.ta.online.model.ReevaluateRiskRequest;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.risk.service.RiskEngineFacade;
import me.asu.ta.rule.model.EvaluationMode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RiskReevaluationServiceTest {
    @Test
    void shouldThrowWhenSnapshotMissing() {
        FeatureQueryService featureQueryService = new FeatureQueryService(null) {
            @Override
            public Optional<AccountFeatureSnapshot> getLatestFeatures(String accountId) {
                return Optional.empty();
            }
        };
        RiskEngineFacade riskEngineFacade = new RiskEngineFacade(null, null, null);
        RiskReevaluationService service = new RiskReevaluationService(featureQueryService, riskEngineFacade);

        assertThrows(ResponseStatusException.class, () -> service.reevaluateAccount("acct-missing", null));
    }

    @Test
    void shouldReevaluateUsingLatestSnapshot() {
        FeatureQueryService featureQueryService = new FeatureQueryService(null) {
            @Override
            public Optional<AccountFeatureSnapshot> getLatestFeatures(String accountId) {
                return Optional.of(AccountFeatureSnapshot.builder("acct-ok", Instant.now()).featureVersion(1).build());
            }
        };
        RiskEngineFacade riskEngineFacade = new RiskEngineFacade(null, null, null) {
            @Override
            public RiskScoreResult evaluateAccount(me.asu.ta.risk.model.RiskEvaluationRequest request) {
                return new RiskScoreResult(
                        1L, "acct-ok", 82.0d, RiskLevel.HIGH, "DEFAULT", 1, Instant.now(),
                        EvaluationMode.REALTIME, List.of("RULE_X"), new ScoreBreakdown(70, 65, 0, 55, 82, "DEFAULT"));
            }
        };
        RiskReevaluationService service = new RiskReevaluationService(featureQueryService, riskEngineFacade);

        RiskScoreResult result = service.reevaluateAccount("acct-ok", new ReevaluateRiskRequest(EvaluationMode.REALTIME, "manual"));

        assertEquals("acct-ok", result.accountId());
        assertEquals(RiskLevel.HIGH, result.riskLevel());
    }
}
