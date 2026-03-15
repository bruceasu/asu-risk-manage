package me.asu.ta.ai.model;

import java.util.Objects;

public record RenderedPrompt(
        String templateCode,
        int templateVersion,
        String renderedContent
) {
    public RenderedPrompt {
        Objects.requireNonNull(templateCode, "templateCode");
        Objects.requireNonNull(renderedContent, "renderedContent");
    }
}
