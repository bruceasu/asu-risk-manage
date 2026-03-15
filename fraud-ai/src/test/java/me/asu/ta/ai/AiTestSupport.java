package me.asu.ta.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import me.asu.ta.ai.client.LlmClient;
import me.asu.ta.ai.client.LlmClientProperties;
import me.asu.ta.ai.model.AiGenerationJob;
import me.asu.ta.ai.model.AiGenerationRequestLog;
import me.asu.ta.ai.model.AiJobStatus;
import me.asu.ta.ai.model.AiRequestStatus;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.ai.model.InvestigationReportStatus;
import me.asu.ta.ai.model.LlmResponse;
import me.asu.ta.ai.model.PromptTemplate;
import me.asu.ta.ai.model.PromptTemplateType;
import me.asu.ta.ai.parser.InvestigationReportParser;
import me.asu.ta.ai.prompt.PromptRenderer;
import me.asu.ta.ai.prompt.PromptTemplateCodes;
import me.asu.ta.ai.prompt.PromptTemplateService;
import me.asu.ta.ai.repository.AiGenerationJobRepository;
import me.asu.ta.ai.repository.AiGenerationRequestLogRepository;
import me.asu.ta.ai.repository.InvestigationCaseBatchReader;
import me.asu.ta.ai.repository.InvestigationReportRepository;
import me.asu.ta.ai.repository.PromptTemplateRepository;
import me.asu.ta.ai.service.AiReportService;
import me.asu.ta.casemanagement.model.CaseFeatureSummary;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.service.CaseFacade;
import me.asu.ta.casemanagement.service.CaseService;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleSeverity;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

public final class AiTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-15T04:00:00Z");

    private AiTestSupport() {
    }

    public static DataSource createDataSource() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        initializeSchema(dataSource);
        return dataSource;
    }

    public static NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    public static PromptTemplateRepository promptTemplateRepository(DataSource dataSource) {
        return new PromptTemplateRepository(jdbcTemplate(dataSource));
    }

    public static AiGenerationRequestLogRepository requestLogRepository(DataSource dataSource) {
        return new AiGenerationRequestLogRepository(jdbcTemplate(dataSource));
    }

    public static InvestigationReportRepository reportRepository(DataSource dataSource) {
        return new InvestigationReportRepository(jdbcTemplate(dataSource));
    }

    public static AiGenerationJobRepository jobRepository(DataSource dataSource) {
        return new AiGenerationJobRepository(jdbcTemplate(dataSource));
    }

    public static InvestigationCaseBatchReader caseBatchReader(DataSource dataSource) {
        return new InvestigationCaseBatchReader(jdbcTemplate(dataSource));
    }

    public static PromptTemplateService promptTemplateService(DataSource dataSource) {
        return new PromptTemplateService(promptTemplateRepository(dataSource));
    }

    public static PromptRenderer promptRenderer() {
        return new PromptRenderer(objectMapper());
    }

    public static InvestigationReportParser reportParser() {
        return new InvestigationReportParser(objectMapper());
    }

    public static LlmClientProperties llmProperties() {
        LlmClientProperties properties = new LlmClientProperties();
        properties.setBaseUrl("http://localhost:18080");
        properties.setApiKey("test-key");
        properties.setModelName("test-model");
        properties.setMaxRetries(1);
        properties.setTemperature(0.1d);
        return properties;
    }

    public static AiReportService aiReportService(DataSource dataSource, CaseFacade caseFacade, LlmClient llmClient) {
        return new AiReportService(
                caseFacade,
                promptTemplateService(dataSource),
                promptRenderer(),
                llmClient,
                llmProperties(),
                reportParser(),
                requestLogRepository(dataSource),
                reportRepository(dataSource),
                new DataSourceTransactionManager(dataSource),
                objectMapper());
    }

    public static InvestigationCaseBundle sampleCaseBundle(long caseId, String accountId) {
        return new InvestigationCaseBundle(
                new InvestigationCase(
                        caseId,
                        accountId,
                        CaseStatus.OPEN,
                        91.0d,
                        RiskLevel.CRITICAL,
                        "DEFAULT",
                        List.of("RULE_TAKEOVER", "GRAPH_HIGH_RISK_NEIGHBORS"),
                        7,
                        EvaluationMode.REALTIME,
                        FIXED_TIME,
                        FIXED_TIME),
                new CaseRiskSummary(caseId, "{\"finalScore\":91.0}", 88.0d, 74.0d, 92.0d, 70.0d, FIXED_TIME),
                new CaseFeatureSummary(caseId, 365, 2, 0.25d, 2, 12.0d, 6, true, 7, 5, 0.93d, FIXED_TIME),
                List.of(new CaseRuleHit(0L, caseId, "ATO_SUSPICION_COMPOSITE", 3, RuleSeverity.CRITICAL, 60, "RULE_TAKEOVER", "Composite takeover suspicion detected", "{\"sharedDeviceAccounts7d\":6}", FIXED_TIME)),
                new CaseGraphSummary(caseId, 74.0d, 7, 5, 6, 3, FIXED_TIME),
                List.of(new CaseTimelineEvent(0L, caseId, FIXED_TIME.minusSeconds(3600), "RULE_HIT", "Takeover risk", "Composite rule hit", "{\"score\":60}", FIXED_TIME)),
                List.of(new CaseRecommendedAction(0L, caseId, "FREEZE_ACCOUNT", "Critical takeover indicators", FIXED_TIME)));
    }

    public static PromptTemplate template(String code, int version, PromptTemplateType type, String content, boolean active) {
        return new PromptTemplate(code, version, type, content, active, FIXED_TIME, FIXED_TIME, "tester", "seed");
    }

    public static void insertDefaultTemplates(DataSource dataSource) {
        PromptTemplateRepository repository = promptTemplateRepository(dataSource);
        repository.save(template(
                PromptTemplateCodes.SYSTEM_REPORT,
                1,
                PromptTemplateType.SYSTEM,
                "You are a fraud investigator. Return strict JSON only.",
                true));
        repository.save(template(
                PromptTemplateCodes.REPORT_FORMAT,
                1,
                PromptTemplateType.REPORT_FORMAT,
                "{\"reportTitle\":\"...\",\"executiveSummary\":\"...\",\"keyRiskIndicators\":\"...\",\"behaviorAnalysis\":\"...\",\"relationshipAnalysis\":\"...\",\"timelineObservations\":\"...\",\"possibleRiskPatterns\":\"...\",\"recommendations\":\"...\"}",
                true));
        repository.save(template(
                PromptTemplateCodes.CASE_RENDERER,
                1,
                PromptTemplateType.CASE_RENDERER,
                "caseId={{caseId}}\naccountId={{accountId}}\nriskLevel={{riskLevel}}\nriskScore={{riskScore}}\nreasons={{topReasonCodes}}\nriskSummary={{riskSummary}}\nfeatureSummary={{featureSummary}}\nruleHits={{ruleHits}}\ngraphSummary={{graphSummary}}\ntimeline={{timeline}}\nrecommendedActions={{recommendedActions}}",
                true));
    }

    public static void insertInvestigationCaseId(DataSource dataSource, long caseId) {
        jdbcTemplate(dataSource).update(
                "insert into investigation_case(case_id) values (:caseId)",
                new MapSqlParameterSource("caseId", caseId));
    }

    public static CaseFacade caseFacadeReturning(InvestigationCaseBundle bundle) {
        CaseService caseService = new CaseService(null, null) {
            @Override
            public Optional<InvestigationCaseBundle> getCaseDetailByCaseId(long caseId) {
                return bundle.investigationCase().caseId() == caseId ? Optional.of(bundle) : Optional.empty();
            }
        };
        return new CaseFacade(caseService);
    }

    public static String successfulRawResponse() {
        return """
                {"id":"resp-1","choices":[{"message":{"content":"{\\"reportTitle\\":\\"Critical takeover case\\",\\"executiveSummary\\":\\"Account shows concentrated takeover risk.\\",\\"keyRiskIndicators\\":\\"High-risk IP login, shared device exposure\\",\\"behaviorAnalysis\\":\\"Withdrawal behavior is abnormal after login anomalies.\\",\\"relationshipAnalysis\\":\\"Graph neighbors overlap with known risky accounts.\\",\\"timelineObservations\\":\\"Risk escalated within one hour of suspicious login.\\",\\"possibleRiskPatterns\\":\\"Account takeover and wallet drain\\",\\"recommendations\\":\\"Freeze account and perform manual review\\"}"}}]}
                """;
    }

    public static LlmResponse successfulLlmResponse() {
        return new LlmResponse(
                "test-model",
                successfulRawResponse(),
                """
                {"reportTitle":"Critical takeover case","executiveSummary":"Account shows concentrated takeover risk.","keyRiskIndicators":"High-risk IP login, shared device exposure","behaviorAnalysis":"Withdrawal behavior is abnormal after login anomalies.","relationshipAnalysis":"Graph neighbors overlap with known risky accounts.","timelineObservations":"Risk escalated within one hour of suspicious login.","possibleRiskPatterns":"Account takeover and wallet drain","recommendations":"Freeze account and perform manual review"}
                """);
    }

    private static void initializeSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table ai_prompt_template (
                        template_code varchar(128) not null,
                        version int not null,
                        template_type varchar(64) not null,
                        template_content clob not null,
                        is_active boolean not null,
                        created_at timestamp not null,
                        updated_at timestamp not null,
                        created_by varchar(128),
                        change_note clob,
                        primary key(template_code, version)
                    )
                    """);
            statement.execute("""
                    create table ai_generation_job (
                        job_id bigint generated by default as identity primary key,
                        job_type varchar(32) not null,
                        started_at timestamp not null,
                        finished_at timestamp,
                        status varchar(32) not null,
                        target_case_count int,
                        processed_case_count int,
                        failed_case_count int,
                        error_message clob
                    )
                    """);
            statement.execute("""
                    create table ai_generation_request_log (
                        request_id bigint generated by default as identity primary key,
                        case_id bigint not null,
                        template_code varchar(128) not null,
                        template_version int not null,
                        model_name varchar(128) not null,
                        request_payload clob not null,
                        requested_at timestamp not null,
                        status varchar(32) not null,
                        error_message clob
                    )
                    """);
            statement.execute("""
                    create table investigation_report (
                        report_id bigint generated by default as identity primary key,
                        case_id bigint not null,
                        report_status varchar(32) not null,
                        report_title varchar(256),
                        executive_summary clob,
                        key_risk_indicators clob,
                        behavior_analysis clob,
                        relationship_analysis clob,
                        timeline_observations clob,
                        possible_risk_patterns clob,
                        recommendations clob,
                        model_name varchar(128) not null,
                        template_code varchar(128) not null,
                        template_version int not null,
                        generated_at timestamp not null,
                        raw_response clob
                    )
                    """);
            statement.execute("""
                    create table investigation_case (
                        case_id bigint primary key
                    )
                    """);
        }
    }
}
