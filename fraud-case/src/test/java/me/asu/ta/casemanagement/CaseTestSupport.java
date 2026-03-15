package me.asu.ta.casemanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import me.asu.ta.casemanagement.builder.FeatureSummaryBuilder;
import me.asu.ta.casemanagement.builder.GraphSummaryBuilder;
import me.asu.ta.casemanagement.builder.InvestigationCaseBuilder;
import me.asu.ta.casemanagement.builder.RiskSummaryBuilder;
import me.asu.ta.casemanagement.builder.RuleSummaryBuilder;
import me.asu.ta.casemanagement.model.CaseFeatureSummary;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import me.asu.ta.casemanagement.recommendation.CaseRecommendationBuilder;
import me.asu.ta.casemanagement.repository.CaseFeatureSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseGenerationJobRepository;
import me.asu.ta.casemanagement.repository.CaseGraphSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRecommendedActionRepository;
import me.asu.ta.casemanagement.repository.CaseRiskSummaryRepository;
import me.asu.ta.casemanagement.repository.CaseRuleHitRepository;
import me.asu.ta.casemanagement.repository.CaseTimelineEventRepository;
import me.asu.ta.casemanagement.repository.InvestigationCaseRepository;
import me.asu.ta.casemanagement.service.CaseRetrievalService;
import me.asu.ta.casemanagement.timeline.CaseTimelineBuilder;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.GraphRiskSignal;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.rule.model.EvaluationMode;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import me.asu.ta.rule.model.RuleSeverity;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public final class CaseTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-15T02:30:00Z");

    private CaseTestSupport() {
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

    public static InvestigationCaseBuilder investigationCaseBuilder() {
        ObjectMapper objectMapper = objectMapper();
        return new InvestigationCaseBuilder(
                new RiskSummaryBuilder(objectMapper),
                new FeatureSummaryBuilder(),
                new RuleSummaryBuilder(objectMapper),
                new GraphSummaryBuilder());
    }

    public static CaseTimelineBuilder timelineBuilder() {
        return new CaseTimelineBuilder(objectMapper());
    }

    public static CaseRecommendationBuilder recommendationBuilder() {
        return new CaseRecommendationBuilder();
    }

    public static InvestigationCaseRepository investigationCaseRepository(DataSource dataSource) {
        return new InvestigationCaseRepository(jdbcTemplate(dataSource));
    }

    public static CaseRiskSummaryRepository caseRiskSummaryRepository(DataSource dataSource) {
        return new CaseRiskSummaryRepository(jdbcTemplate(dataSource));
    }

    public static CaseFeatureSummaryRepository caseFeatureSummaryRepository(DataSource dataSource) {
        return new CaseFeatureSummaryRepository(jdbcTemplate(dataSource));
    }

    public static CaseRuleHitRepository caseRuleHitRepository(DataSource dataSource) {
        return new CaseRuleHitRepository(jdbcTemplate(dataSource));
    }

    public static CaseGraphSummaryRepository caseGraphSummaryRepository(DataSource dataSource) {
        return new CaseGraphSummaryRepository(jdbcTemplate(dataSource));
    }

    public static CaseTimelineEventRepository caseTimelineEventRepository(DataSource dataSource) {
        return new CaseTimelineEventRepository(jdbcTemplate(dataSource));
    }

    public static CaseRecommendedActionRepository caseRecommendedActionRepository(DataSource dataSource) {
        return new CaseRecommendedActionRepository(jdbcTemplate(dataSource));
    }

    public static CaseGenerationJobRepository caseGenerationJobRepository(DataSource dataSource) {
        return new CaseGenerationJobRepository(jdbcTemplate(dataSource));
    }

    public static CaseRetrievalService caseRetrievalService(DataSource dataSource) {
        return new CaseRetrievalService(
                investigationCaseRepository(dataSource),
                caseRiskSummaryRepository(dataSource),
                caseFeatureSummaryRepository(dataSource),
                caseRuleHitRepository(dataSource),
                caseGraphSummaryRepository(dataSource),
                caseTimelineEventRepository(dataSource),
                caseRecommendedActionRepository(dataSource));
    }

    public static AccountFeatureSnapshot.Builder snapshotBuilder(String accountId) {
        return AccountFeatureSnapshot.builder(accountId, FIXED_TIME)
                .featureVersion(7)
                .accountAgeDays(365)
                .kycLevelNumeric(2)
                .registrationIpRiskScore(0.15d)
                .loginCount24h(12)
                .loginFailureCount24h(3)
                .loginFailureRate24h(0.25d)
                .uniqueIpCount24h(4)
                .highRiskIpLoginCount24h(2)
                .vpnIpLoginCount24h(1)
                .newDeviceLoginCount7d(2)
                .nightLoginRatio7d(0.45d)
                .transactionCount24h(8)
                .totalAmount24h(14500.0d)
                .avgTransactionAmount24h(5200.0d)
                .depositCount24h(2)
                .withdrawCount24h(1)
                .depositAmount24h(9000.0d)
                .withdrawAmount24h(7000.0d)
                .depositWithdrawRatio24h(0.78d)
                .uniqueCounterpartyCount24h(9)
                .withdrawAfterDepositDelayAvg24h(12.0d)
                .rapidWithdrawAfterDepositFlag24h(true)
                .rewardTransactionCount30d(2)
                .rewardWithdrawDelayAvg30d(8.0d)
                .uniqueDeviceCount7d(3)
                .deviceSwitchCount24h(2)
                .sharedDeviceAccounts7d(6)
                .securityEventCount24h(2)
                .rapidProfileChangeFlag24h(true)
                .securityChangeBeforeWithdrawFlag24h(true)
                .sharedIpAccounts7d(4)
                .sharedBankAccounts30d(3)
                .graphClusterSize30d(7)
                .riskNeighborCount30d(5)
                .anomalyScoreLast(0.93d);
    }

    public static RuleEngineResult sampleRuleEngineResult(String accountId) {
        return new RuleEngineResult(
                accountId,
                EvaluationMode.REALTIME,
                FIXED_TIME,
                88,
                List.of(
                        new RuleEvaluationResult(
                                "ATO_SUSPICION_COMPOSITE",
                                3,
                                true,
                                RuleSeverity.CRITICAL,
                                60,
                                "RULE_TAKEOVER",
                                "Composite takeover suspicion detected",
                                Map.of("sharedDeviceAccounts7d", 6, "highRiskIpLoginCount24h", 2)),
                        new RuleEvaluationResult(
                                "RAPID_WITHDRAW_AFTER_DEPOSIT",
                                2,
                                true,
                                RuleSeverity.HIGH,
                                28,
                                "RULE_WALLET_DRAIN",
                                "Rapid withdrawal after deposit observed",
                                Map.of("withdrawAfterDepositDelayAvg24h", 12.0d))),
                List.of("RULE_TAKEOVER", "RULE_WALLET_DRAIN"));
    }

    public static RiskScoreResult sampleRiskScoreResult(String accountId) {
        return new RiskScoreResult(
                0L,
                accountId,
                91.0d,
                RiskLevel.CRITICAL,
                "DEFAULT",
                7,
                FIXED_TIME,
                EvaluationMode.REALTIME,
                List.of("GRAPH_HIGH_RISK_NEIGHBORS", "RULE_TAKEOVER", "BEHAVIOR_SHARED_DEVICE_EXPOSURE"),
                new ScoreBreakdown(88.0d, 74.0d, 93.0d, 70.0d, 91.0d, "DEFAULT"));
    }

    public static GraphRiskSignal sampleGraphSignal() {
        return new GraphRiskSignal(74.0d, 7, 5, 6, 3);
    }

    public static void insertAccountSnapshot(DataSource dataSource, String accountId) {
        jdbcTemplate(dataSource).update("""
                insert into account_feature_snapshot(account_id)
                values (:accountId)
                """, new MapSqlParameterSource("accountId", accountId));
    }

    public static InvestigationCaseBundle persistCaseBundle(DataSource dataSource, String accountId) {
        InvestigationCaseRepository caseRepository = investigationCaseRepository(dataSource);
        CaseRiskSummaryRepository riskRepository = caseRiskSummaryRepository(dataSource);
        CaseFeatureSummaryRepository featureRepository = caseFeatureSummaryRepository(dataSource);
        CaseRuleHitRepository hitRepository = caseRuleHitRepository(dataSource);
        CaseGraphSummaryRepository graphRepository = caseGraphSummaryRepository(dataSource);
        CaseTimelineEventRepository timelineRepository = caseTimelineEventRepository(dataSource);
        CaseRecommendedActionRepository actionRepository = caseRecommendedActionRepository(dataSource);

        AccountFeatureSnapshot snapshot = snapshotBuilder(accountId).build();
        RuleEngineResult ruleEngineResult = sampleRuleEngineResult(accountId);
        RiskScoreResult riskScoreResult = sampleRiskScoreResult(accountId);
        InvestigationCaseBuilder builder = investigationCaseBuilder();

        InvestigationCase savedCase = caseRepository.save(builder.buildCaseHeader(snapshot, riskScoreResult));
        long caseId = savedCase.caseId();
        CaseRiskSummary riskSummary = builder.buildRiskSummary(caseId, riskScoreResult);
        CaseFeatureSummary featureSummary = builder.buildFeatureSummary(caseId, snapshot, FIXED_TIME);
        List<CaseRuleHit> ruleHits = builder.buildRuleHits(caseId, ruleEngineResult, FIXED_TIME);
        CaseGraphSummary graphSummary = builder.buildGraphSummary(
                caseId,
                new me.asu.ta.casemanagement.model.CaseGenerationRequest(
                        accountId,
                        snapshot,
                        sampleGraphSignal(),
                        null,
                        Map.of(),
                        EvaluationMode.REALTIME,
                        FIXED_TIME),
                FIXED_TIME);
        List<CaseTimelineEvent> timeline = timelineBuilder().build(caseId, snapshot, ruleEngineResult, riskScoreResult);
        List<CaseRecommendedAction> actions = recommendationBuilder().build(caseId, snapshot, ruleEngineResult, riskScoreResult);

        riskRepository.save(riskSummary);
        featureRepository.save(featureSummary);
        graphRepository.save(graphSummary);
        hitRepository.insertBatch(ruleHits);
        timelineRepository.insertBatch(timeline);
        actionRepository.insertBatch(actions);

        return caseRetrievalService(dataSource).getCaseById(caseId).orElseThrow();
    }

    private static void initializeSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table investigation_case (
                        case_id bigint generated by default as identity primary key,
                        account_id varchar(64) not null,
                        case_status varchar(64) not null,
                        risk_score double precision not null,
                        risk_level varchar(32) not null,
                        profile_name varchar(64) not null,
                        top_reason_codes clob,
                        feature_version int not null,
                        evaluation_mode varchar(32) not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            statement.execute("create index idx_investigation_case_account on investigation_case(account_id)");
            statement.execute("""
                    create table case_risk_summary (
                        case_id bigint primary key,
                        score_breakdown_json clob not null,
                        rule_score double precision,
                        graph_score double precision,
                        anomaly_score double precision,
                        behavior_score double precision,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_feature_summary (
                        case_id bigint primary key,
                        account_age_days int,
                        high_risk_ip_login_count_24h int,
                        login_failure_rate_24h double precision,
                        new_device_login_count_7d int,
                        withdraw_after_deposit_delay_avg_24h double precision,
                        shared_device_accounts_7d int,
                        security_change_before_withdraw_flag_24h boolean,
                        graph_cluster_size_30d int,
                        risk_neighbor_count_30d int,
                        anomaly_score_last double precision,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_rule_hit (
                        case_rule_hit_id bigint generated by default as identity primary key,
                        case_id bigint not null,
                        rule_code varchar(128) not null,
                        rule_version int not null,
                        severity varchar(32) not null,
                        score int not null,
                        reason_code varchar(128) not null,
                        message clob,
                        evidence_json clob,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_graph_summary (
                        case_id bigint primary key,
                        graph_score double precision,
                        graph_cluster_size int,
                        risk_neighbor_count int,
                        shared_device_accounts int,
                        shared_bank_accounts int,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_timeline_event (
                        timeline_event_id bigint generated by default as identity primary key,
                        case_id bigint not null,
                        event_time timestamp not null,
                        event_type varchar(64) not null,
                        title varchar(256) not null,
                        description clob,
                        evidence_json clob,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_recommended_action (
                        case_action_id bigint generated by default as identity primary key,
                        case_id bigint not null,
                        action_code varchar(64) not null,
                        action_reason clob,
                        created_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table case_generation_job (
                        job_id bigint generated by default as identity primary key,
                        job_type varchar(32) not null,
                        started_at timestamp not null,
                        finished_at timestamp,
                        status varchar(32) not null,
                        target_account_count int,
                        processed_account_count int,
                        failed_account_count int,
                        error_message clob
                    )
                    """);
            statement.execute("""
                    create table account_feature_snapshot (
                        account_id varchar(64) primary key
                    )
                    """);
        }
    }
}
