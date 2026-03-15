package me.asu.ta.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.PromptTemplateType;
import me.asu.ta.ai.model.RenderedPrompt;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import org.junit.jupiter.api.Test;

class PromptRendererTest {
    @Test
    void shouldRenderPromptDeterministically() {
        InvestigationCaseBundle bundle = AiTestSupport.sampleCaseBundle(101L, "acct-ai-1");
        PromptRenderer renderer = AiTestSupport.promptRenderer();
        var template = AiTestSupport.template(
                PromptTemplateCodes.CASE_RENDERER,
                1,
                PromptTemplateType.CASE_RENDERER,
                "case={{caseId}} account={{accountId}} risk={{riskLevel}} rules={{ruleHits}}",
                true);

        RenderedPrompt first = renderer.renderCasePrompt(template, bundle);
        RenderedPrompt second = renderer.renderCasePrompt(template, bundle);

        assertEquals(first.renderedContent(), second.renderedContent());
        assertTrue(first.renderedContent().contains("acct-ai-1"));
        assertTrue(first.renderedContent().contains("ATO_SUSPICION_COMPOSITE"));
    }
}
