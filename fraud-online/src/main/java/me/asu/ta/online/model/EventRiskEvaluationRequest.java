package me.asu.ta.online.model;

import java.time.Instant;
import java.util.Map;

public record EventRiskEvaluationRequest(
        String accountId,
        EventRiskType eventType,
        Instant eventTime,
        String source,
        Double amount,
        String deviceId,
        Map<String, Object> attributes
) {
    public EventRiskEvaluationRequest {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
