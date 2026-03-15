package me.asu.ta.ai.model;

import java.util.Objects;

public record LlmRequest(
        String modelName,
        String systemPrompt,
        String userPrompt,
        double temperature
) {
    public LlmRequest {
        Objects.requireNonNull(modelName, "modelName");
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        Objects.requireNonNull(userPrompt, "userPrompt");
        temperature = Math.max(0.0d, temperature);
    }
}
