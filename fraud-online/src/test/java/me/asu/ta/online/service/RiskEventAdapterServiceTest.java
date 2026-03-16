package me.asu.ta.online.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import me.asu.ta.feature.service.FeatureQueryService;
import me.asu.ta.online.model.EventRiskEvaluationRequest;
import me.asu.ta.online.model.EventRiskType;
import me.asu.ta.risk.service.RiskEngineFacade;
import org.junit.jupiter.api.Test;

class RiskEventAdapterServiceTest {
    @Test
    void shouldMapWithdrawEventSignals() {
        RiskEventAdapterService service = new RiskEventAdapterService(
                new FeatureQueryService(null),
                new RiskEngineFacade(null, null, null));
        Map<String, Object> signals = service.toContextSignals(new EventRiskEvaluationRequest(
                "acct-event",
                EventRiskType.WITHDRAW_REQUEST,
                null,
                "portal",
                5000.0d,
                null,
                Map.of("channel", "manual")));

        assertEquals("WITHDRAW_REQUEST", signals.get("online.event.type"));
        assertEquals(5000.0d, signals.get("online.event.amount"));
        assertTrue((Boolean) signals.get("online.event.requiresWithdrawReview"));
        assertEquals("manual", signals.get("channel"));
    }
}
