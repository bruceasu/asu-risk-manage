package me.asu.ta.ai.model;

import java.util.Objects;

public record LlmResponse(
        String modelName,
        String rawResponse,
        String content
) {
    public LlmResponse {
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(rawResponse, "rawResponse");
        Objects.requireNonNull(content, "content");
    }
}
