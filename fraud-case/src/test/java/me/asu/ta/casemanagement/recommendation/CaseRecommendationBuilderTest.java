package me.asu.ta.casemanagement.recommendation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.asu.ta.casemanagement.CaseTestSupport;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import org.junit.jupiter.api.Test;

class CaseRecommendationBuilderTest {
    @Test
    void shouldGenerateExplicitRecommendedActionsForCriticalCase() {
        CaseRecommendationBuilder builder = CaseTestSupport.recommendationBuilder();

        List<CaseRecommendedAction> actions = builder.build(
                88L,
                CaseTestSupport.snapshotBuilder("acct-recommendation").build(),
                CaseTestSupport.sampleRuleEngineResult("acct-recommendation"),
                CaseTestSupport.sampleRiskScoreResult("acct-recommendation"));

        List<String> actionCodes = actions.stream().map(CaseRecommendedAction::actionCode).toList();
        assertTrue(actionCodes.contains("FREEZE_ACCOUNT"));
        assertTrue(actionCodes.contains("MANUAL_REVIEW"));
        assertTrue(actionCodes.contains("REVIEW_SECURITY_CHANGES"));
        assertTrue(actionCodes.contains("INVESTIGATE_LINKED_ACCOUNTS"));
    }
}
