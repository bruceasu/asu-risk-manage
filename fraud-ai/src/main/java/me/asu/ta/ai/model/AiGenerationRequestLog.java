package me.asu.ta.ai.model;

import java.time.Instant;
import java.util.Objects;

public record AiGenerationRequestLog(
        Long requestId,
        long caseId,
        String templateCode,
        int templateVersion,
        String modelName,
        String requestPayload,
        Instant requestedAt,
        AiRequestStatus status,
        String errorMessage
) {
    public AiGenerationRequestLog {
        Objects.requireNonNull(templateCode, "templateCode");
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(requestPayload, "requestPayload");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(status, "status");
    }
}
