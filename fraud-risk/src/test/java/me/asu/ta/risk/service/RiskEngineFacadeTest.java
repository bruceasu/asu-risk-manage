package me.asu.ta.risk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.RiskEvaluationRequest;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineFacadeContext;
import me.asu.ta.rule.model.RuleEngineResult;
import org.junit.jupiter.api.Test;

class RiskEngineFacadeTest {
    @Test
    void shouldPassResolvedGraphSignalsIntoRuleEngineContext() {
        CapturingRuleEngineFacade ruleEngineFacade = new CapturingRuleEngineFacade();
        StubRiskEvaluationService riskEvaluationService = new StubRiskEvaluationService();
        RiskEngineFacade facade = new RiskEngineFacade(
                ruleEngineFacade,
                riskEvaluationService,
                new GraphRiskSignalResolver());

        AccountFeatureSnapshot snapshot = RiskTestSupport.snapshotBuilder("acct-risk-facade")
                .riskNeighborCount30d(4)
                .graphClusterSize30d(6)
                .sharedDeviceAccounts7d(5)
                .sharedBankAccounts30d(3)
                .build();
        RiskEvaluationRequest request = new RiskEvaluationRequest(
                snapshot.accountId(),
                snapshot,
                null,
                null,
                Map.of("channel", "web"),
                EvaluationMode.REALTIME,
                RiskTestSupport.FIXED_TIME);

        facade.evaluateAccount(request);

        assertNotNull(ruleEngineFacade.capturedContext);
        assertEquals(100.0d, ruleEngineFacade.capturedContext.graphSignals().get("graphScore"));
        assertEquals(6, ruleEngineFacade.capturedContext.graphSignals().get("graphClusterSize"));
        assertEquals(4, ruleEngineFacade.capturedContext.graphSignals().get("riskNeighborCount"));
        assertEquals(5, ruleEngineFacade.capturedContext.graphSignals().get("sharedDeviceAccounts"));
        assertEquals(3, ruleEngineFacade.capturedContext.graphSignals().get("sharedBankAccounts"));
    }

    private static final class CapturingRuleEngineFacade extends me.asu.ta.rule.service.RuleEngineFacade {
        private RuleEngineFacadeContext capturedContext;

        private CapturingRuleEngineFacade() {
            super(null);
        }

        @Override
        public RuleEngineResult evaluateAccount(String accountId, RuleEngineFacadeContext context) {
            this.capturedContext = context;
            return new RuleEngineResult(
                    accountId,
                    EvaluationMode.REALTIME,
                    context.resolvedEvaluationTime(),
                    0,
                    List.of(),
                    List.of());
        }
    }

    private static final class StubRiskEvaluationService extends RiskEvaluationService {
        private StubRiskEvaluationService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public RiskScoreResult evaluateAccountRisk(RiskEvaluationRequest request, RuleEngineResult ruleEngineResult) {
            return new RiskScoreResult(
                    0L,
                    request.accountId(),
                    0.0d,
                    me.asu.ta.risk.model.RiskLevel.LOW,
                    "TEST",
                    request.snapshot().featureVersion(),
                    request.resolvedEvaluationTime(),
                    request.resolvedEvaluationMode(),
                    List.of(),
                    new me.asu.ta.risk.model.ScoreBreakdown(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, "TEST"));
        }
    }
}
