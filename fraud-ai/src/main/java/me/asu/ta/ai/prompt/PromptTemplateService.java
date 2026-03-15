package me.asu.ta.ai.prompt;

import java.util.List;
import java.util.Objects;
import me.asu.ta.ai.model.PromptTemplate;
import me.asu.ta.ai.model.PromptTemplateType;
import me.asu.ta.ai.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {
    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    public PromptTemplate findActiveTemplate(String templateCode, PromptTemplateType templateType) {
        Objects.requireNonNull(templateCode, "templateCode");
        Objects.requireNonNull(templateType, "templateType");
        return promptTemplateRepository.findActiveTemplate(templateCode, templateType)
                .orElseThrow(() -> new IllegalStateException("Active prompt template not found: " + templateCode));
    }

    public List<PromptTemplate> findByTemplateCode(String templateCode) {
        Objects.requireNonNull(templateCode, "templateCode");
        return promptTemplateRepository.findByTemplateCode(templateCode);
    }
}
