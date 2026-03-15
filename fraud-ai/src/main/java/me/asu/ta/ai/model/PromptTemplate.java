package me.asu.ta.ai.model;

import java.time.Instant;
import java.util.Objects;

public record PromptTemplate(
        String templateCode,
        int version,
        PromptTemplateType templateType,
        String templateContent,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String changeNote
) {
    public PromptTemplate {
        Objects.requireNonNull(templateCode, "templateCode");
        Objects.requireNonNull(templateType, "templateType");
        Objects.requireNonNull(templateContent, "templateContent");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
