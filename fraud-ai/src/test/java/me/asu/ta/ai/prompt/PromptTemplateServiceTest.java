package me.asu.ta.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;
import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.PromptTemplateType;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {
    @Test
    void shouldLoadHighestActiveTemplateVersion() throws Exception {
        DataSource dataSource = AiTestSupport.createDataSource();
        AiTestSupport.promptTemplateRepository(dataSource).save(AiTestSupport.template(
                PromptTemplateCodes.CASE_RENDERER, 1, PromptTemplateType.CASE_RENDERER, "v1", true));
        AiTestSupport.promptTemplateRepository(dataSource).save(AiTestSupport.template(
                PromptTemplateCodes.CASE_RENDERER, 2, PromptTemplateType.CASE_RENDERER, "v2", true));

        String content = AiTestSupport.promptTemplateService(dataSource)
                .findActiveTemplate(PromptTemplateCodes.CASE_RENDERER, PromptTemplateType.CASE_RENDERER)
                .templateContent();

        assertEquals("v2", content);
    }
}
