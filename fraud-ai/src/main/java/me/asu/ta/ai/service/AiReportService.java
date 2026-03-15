package me.asu.ta.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.asu.ta.ai.client.LlmClient;
import me.asu.ta.ai.client.LlmClientProperties;
import me.asu.ta.ai.model.AiGenerationRequestLog;
import me.asu.ta.ai.model.AiRequestStatus;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.ai.model.LlmRequest;
import me.asu.ta.ai.model.LlmResponse;
import me.asu.ta.ai.model.PromptTemplate;
import me.asu.ta.ai.model.PromptTemplateType;
import me.asu.ta.ai.model.RenderedPrompt;
import me.asu.ta.ai.parser.InvestigationReportParser;
import me.asu.ta.ai.prompt.PromptRenderer;
import me.asu.ta.ai.prompt.PromptTemplateCodes;
import me.asu.ta.ai.prompt.PromptTemplateService;
import me.asu.ta.ai.repository.AiGenerationRequestLogRepository;
import me.asu.ta.ai.repository.InvestigationReportRepository;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiReportService {
    private static final Logger log = LoggerFactory.getLogger(AiReportService.class);
    private final CaseFacade caseFacade;
    private final PromptTemplateService promptTemplateService;
    private final PromptRenderer promptRenderer;
    private final LlmClient llmClient;
    private final LlmClientProperties properties;
    private final InvestigationReportParser investigationReportParser;
    private final AiGenerationRequestLogRepository requestLogRepository;
    private final InvestigationReportRepository investigationReportRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public AiReportService(
            CaseFacade caseFacade,
            PromptTemplateService promptTemplateService,
            PromptRenderer promptRenderer,
            LlmClient llmClient,
            LlmClientProperties properties,
            InvestigationReportParser investigationReportParser,
            AiGenerationRequestLogRepository requestLogRepository,
            InvestigationReportRepository investigationReportRepository,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper) {
        this.caseFacade = caseFacade;
        this.promptTemplateService = promptTemplateService;
        this.promptRenderer = promptRenderer;
        this.llmClient = llmClient;
        this.properties = properties;
        this.investigationReportParser = investigationReportParser;
        this.requestLogRepository = requestLogRepository;
        this.investigationReportRepository = investigationReportRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    public InvestigationReport generateReport(long caseId) {
        InvestigationCaseBundle caseBundle = caseFacade.getCaseDetailByCaseId(caseId)
                .orElseThrow(() -> new IllegalStateException("Case not found: " + caseId));
        return generateReportForCase(caseBundle);
    }

    public InvestigationReport generateReportForCase(InvestigationCaseBundle caseBundle) {
        PromptTemplate systemTemplate = promptTemplateService.findActiveTemplate(
                PromptTemplateCodes.SYSTEM_REPORT,
                PromptTemplateType.SYSTEM);
        PromptTemplate formatTemplate = promptTemplateService.findActiveTemplate(
                PromptTemplateCodes.REPORT_FORMAT,
                PromptTemplateType.REPORT_FORMAT);
        PromptTemplate caseTemplate = promptTemplateService.findActiveTemplate(
                PromptTemplateCodes.CASE_RENDERER,
                PromptTemplateType.CASE_RENDERER);

        RenderedPrompt systemPrompt = promptRenderer.renderStaticPrompt(systemTemplate);
        RenderedPrompt reportFormatPrompt = promptRenderer.renderStaticPrompt(formatTemplate);
        RenderedPrompt casePrompt = promptRenderer.renderCasePrompt(caseTemplate, caseBundle);
        String userPrompt = promptRenderer.combineUserPrompt(reportFormatPrompt, casePrompt);

        LlmRequest llmRequest = new LlmRequest(
                properties.getModelName(),
                systemPrompt.renderedContent(),
                userPrompt,
                properties.getTemperature());
        AiGenerationRequestLog requestLog = requestLogRepository.save(new AiGenerationRequestLog(
                null,
                caseBundle.investigationCase().caseId(),
                casePrompt.templateCode(),
                casePrompt.templateVersion(),
                properties.getModelName(),
                toPayload(llmRequest),
                Instant.now(),
                AiRequestStatus.PENDING,
                null));
        long requestId = requestLog.requestId() == null ? 0L : requestLog.requestId();

        try {
            log.info("Generating AI report, caseId={}, templateCode={}, templateVersion={}, modelName={}, requestId={}",
                    caseBundle.investigationCase().caseId(),
                    casePrompt.templateCode(),
                    casePrompt.templateVersion(),
                    properties.getModelName(),
                    requestId);
            LlmResponse llmResponse = llmClient.generate(llmRequest);
            InvestigationReport parsedReport = investigationReportParser.parse(caseBundle.investigationCase().caseId(), casePrompt, llmResponse);
            return transactionTemplate.execute(status -> {
                requestLogRepository.updateStatus(requestId, AiRequestStatus.SUCCESS, null);
                return investigationReportRepository.save(parsedReport);
            });
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status ->
                    requestLogRepository.updateStatus(requestId, AiRequestStatus.FAILED, ex.getMessage()));
            throw ex;
        }
    }

    public Map<Long, InvestigationReport> generateBatchReports(List<Long> caseIds) {
        Map<Long, InvestigationReport> reports = new LinkedHashMap<>();
        for (Long caseId : caseIds) {
            try {
                reports.put(caseId, generateReport(caseId));
            } catch (RuntimeException ex) {
                log.error("Failed to generate AI report for caseId={}", caseId, ex);
            }
        }
        return reports;
    }

    public Optional<InvestigationReport> findLatestReportByCaseId(long caseId) {
        return investigationReportRepository.findLatestByCaseId(caseId);
    }

    private String toPayload(LlmRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize LLM request payload", ex);
        }
    }
}
