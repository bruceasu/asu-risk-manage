package me.asu.ta.casemanagement.timeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.asu.ta.casemanagement.CaseTestSupport;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import org.junit.jupiter.api.Test;

class CaseTimelineBuilderTest {
    @Test
    void shouldGenerateInvestigationOrientedTimelineInAscendingOrder() {
        CaseTimelineBuilder builder = CaseTestSupport.timelineBuilder();

        List<CaseTimelineEvent> timeline = builder.build(
                100L,
                CaseTestSupport.snapshotBuilder("acct-timeline").build(),
                CaseTestSupport.sampleRuleEngineResult("acct-timeline"),
                CaseTestSupport.sampleRiskScoreResult("acct-timeline"));

        assertTrue(timeline.size() >= 8);
        assertEquals("LOGIN_PATTERN", timeline.getFirst().eventType());
        assertTrue(timeline.stream().anyMatch(event -> "CASE_CREATED".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "HIGH_VALUE_TRANSFER".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "RULE_HIT".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "NEW_DEVICE_LOGIN".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "PASSWORD_RESET".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "DEPOSIT_ACTIVITY".equals(event.eventType())));
        assertTrue(timeline.stream().anyMatch(event -> "WITHDRAWAL_ACTIVITY".equals(event.eventType())));
        for (int i = 1; i < timeline.size(); i++) {
            assertTrue(!timeline.get(i).eventTime().isBefore(timeline.get(i - 1).eventTime()));
        }
    }
}
