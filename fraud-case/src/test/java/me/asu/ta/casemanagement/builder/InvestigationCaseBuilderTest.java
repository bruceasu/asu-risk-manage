package me.asu.ta.casemanagement.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.asu.ta.casemanagement.CaseTestSupport;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineResult;
import org.junit.jupiter.api.Test;

class InvestigationCaseBuilderTest {
    @Test
    void shouldBuildCaseArtifactsFromSampleInputs() {
        InvestigationCaseBuilder builder = CaseTestSupport.investigationCaseBuilder();
        AccountFeatureSnapshot snapshot = CaseTestSupport.snapshotBuilder("acct-case-build").build();
        RiskScoreResult riskScoreResult = CaseTestSupport.sampleRiskScoreResult("acct-case-build");
        RuleEngineResult ruleEngineResult = CaseTestSupport.sampleRuleEngineResult("acct-case-build");

        InvestigationCase investigationCase = builder.buildCaseHeader(snapshot, riskScoreResult);
        CaseRiskSummary riskSummary = builder.buildRiskSummary(42L, riskScoreResult);
        CaseGraphSummary graphSummary = builder.buildGraphSummary(
                42L,
                new CaseGenerationRequest(
                        "acct-case-build",
                        snapshot,
                        CaseTestSupport.sampleGraphSignal(),
                        null,
                        java.util.Map.of(),
                        EvaluationMode.REALTIME,
                        CaseTestSupport.FIXED_TIME),
                CaseTestSupport.FIXED_TIME);

        assertEquals("acct-case-build", investigationCase.accountId());
        assertEquals(CaseStatus.OPEN, investigationCase.caseStatus());
        assertEquals(91.0d, investigationCase.riskScore());
        assertEquals(3, investigationCase.topReasonCodes().size());
        assertEquals(7, investigationCase.featureVersion());

        assertEquals(88.0d, riskSummary.ruleScore());
        assertTrue(riskSummary.scoreBreakdownJson().contains("\"profileName\":\"DEFAULT\""));

        assertEquals(74.0d, graphSummary.graphScore());
        assertEquals(2, builder.buildRuleHits(42L, ruleEngineResult, CaseTestSupport.FIXED_TIME).size());
    }
}
