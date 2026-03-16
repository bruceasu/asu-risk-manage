package me.asu.ta.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.classification.RiskLevelClassifier;
import me.asu.ta.risk.model.RiskReasonMapping;
import me.asu.ta.risk.model.RiskWeightProfile;
import me.asu.ta.risk.reason.RiskReasonGenerator;
import me.asu.ta.risk.repository.RiskReasonMappingRepository;
import me.asu.ta.risk.repository.RiskScoreResultRepository;
import me.asu.ta.risk.repository.RiskWeightProfileRepository;
import me.asu.ta.risk.scoring.BehaviorScoreCalculator;
import me.asu.ta.risk.scoring.RiskScoreCalculator;
import me.asu.ta.risk.scoring.RiskWeightProfileService;
import me.asu.ta.risk.service.GraphRiskSignalResolver;
import me.asu.ta.risk.service.RiskEvaluationService;
import me.asu.ta.risk.service.RiskScoreResultFactory;
import me.asu.ta.rule.model.RuleSeverity;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public final class RiskTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-15T00:00:00Z");

    private RiskTestSupport() {
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

    public static RiskWeightProfileRepository riskWeightProfileRepository(DataSource dataSource) {
        return new RiskWeightProfileRepository(jdbcTemplate(dataSource));
    }

    public static RiskReasonMappingRepository riskReasonMappingRepository(DataSource dataSource) {
        return new RiskReasonMappingRepository(jdbcTemplate(dataSource));
    }

    public static RiskScoreResultRepository riskScoreResultRepository(DataSource dataSource) {
        return new RiskScoreResultRepository(jdbcTemplate(dataSource), new ObjectMapper());
    }

    public static RiskEvaluationService riskEvaluationService(DataSource dataSource) {
        RiskWeightProfileRepository weightProfileRepository = riskWeightProfileRepository(dataSource);
        RiskReasonMappingRepository reasonMappingRepository = riskReasonMappingRepository(dataSource);
        RiskScoreResultRepository scoreResultRepository = riskScoreResultRepository(dataSource);
        return new RiskEvaluationService(
                new BehaviorScoreCalculator(),
                new GraphRiskSignalResolver(),
                new RiskWeightProfileService(weightProfileRepository),
                new RiskScoreCalculator(),
                new RiskReasonGenerator(reasonMappingRepository),
                new RiskScoreResultFactory(new RiskLevelClassifier()),
                scoreResultRepository);
    }

    public static void insertWeightProfile(DataSource dataSource, RiskWeightProfile profile) {
        jdbcTemplate(dataSource).update("""
                insert into risk_weight_profile(
                    profile_name, rule_weight, graph_weight, anomaly_weight,
                    behavior_weight, enabled, created_at, updated_at
                ) values (
                    :profileName, :ruleWeight, :graphWeight, :anomalyWeight,
                    :behaviorWeight, :enabled, :createdAt, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("profileName", profile.profileName())
                .addValue("ruleWeight", profile.ruleWeight())
                .addValue("graphWeight", profile.graphWeight())
                .addValue("anomalyWeight", profile.anomalyWeight())
                .addValue("behaviorWeight", profile.behaviorWeight())
                .addValue("enabled", profile.enabled())
                .addValue("createdAt", profile.createdAt())
                .addValue("updatedAt", profile.updatedAt()));
    }

    public static void insertReasonMapping(DataSource dataSource, RiskReasonMapping mapping) {
        jdbcTemplate(dataSource).update("""
                insert into risk_reason_mapping(
                    reason_code, reason_title, reason_description, severity,
                    category, created_at, updated_at
                ) values (
                    :reasonCode, :reasonTitle, :reasonDescription, :severity,
                    :category, :createdAt, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("reasonCode", mapping.reasonCode())
                .addValue("reasonTitle", mapping.reasonTitle())
                .addValue("reasonDescription", mapping.reasonDescription())
                .addValue("severity", mapping.severity().name())
                .addValue("category", mapping.category())
                .addValue("createdAt", mapping.createdAt())
                .addValue("updatedAt", mapping.updatedAt()));
    }

    public static AccountFeatureSnapshot.Builder snapshotBuilder(String accountId) {
        return AccountFeatureSnapshot.builder(accountId, FIXED_TIME)
                .featureVersion(3)
                .accountAgeDays(240)
                .kycLevelNumeric(2)
                .registrationIpRiskScore(0.08d)
                .loginCount24h(8)
                .loginFailureCount24h(1)
                .loginFailureRate24h(0.12d)
                .uniqueIpCount24h(2)
                .highRiskIpLoginCount24h(0)
                .vpnIpLoginCount24h(0)
                .newDeviceLoginCount7d(0)
                .nightLoginRatio7d(0.05d)
                .transactionCount24h(4)
                .totalAmount24h(860.0d)
                .avgTransactionAmount24h(215.0d)
                .depositCount24h(1)
                .withdrawCount24h(1)
                .depositAmount24h(500.0d)
                .withdrawAmount24h(120.0d)
                .depositWithdrawRatio24h(0.24d)
                .uniqueCounterpartyCount24h(3)
                .withdrawAfterDepositDelayAvg24h(120.0d)
                .rapidWithdrawAfterDepositFlag24h(false)
                .rewardTransactionCount30d(0)
                .rewardWithdrawDelayAvg30d(72.0d)
                .uniqueDeviceCount7d(1)
                .deviceSwitchCount24h(0)
                .sharedDeviceAccounts7d(1)
                .securityEventCount24h(0)
                .rapidProfileChangeFlag24h(false)
                .securityChangeBeforeWithdrawFlag24h(false)
                .sharedIpAccounts7d(1)
                .sharedBankAccounts30d(1)
                .graphClusterSize30d(2)
                .riskNeighborCount30d(1);
    }

    public static RiskWeightProfile defaultProfile() {
        return new RiskWeightProfile("DEFAULT", 0.40d, 0.25d, 0.20d, 0.15d, true, FIXED_TIME, FIXED_TIME);
    }

    public static RiskWeightProfile noMlProfile() {
        return new RiskWeightProfile("NO_ML", 0.55d, 0.30d, 0.0d, 0.15d, true, FIXED_TIME, FIXED_TIME);
    }

    public static RiskReasonMapping reasonMapping(String reasonCode, RuleSeverity severity, String category) {
        return new RiskReasonMapping(
                reasonCode,
                reasonCode + "_TITLE",
                reasonCode + "_DESCRIPTION",
                severity,
                category,
                FIXED_TIME,
                FIXED_TIME);
    }

    private static void initializeSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table risk_weight_profile (
                        profile_name varchar(64) primary key,
                        rule_weight double precision not null,
                        graph_weight double precision not null,
                        anomaly_weight double precision not null,
                        behavior_weight double precision not null,
                        enabled boolean not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table risk_reason_mapping (
                        reason_code varchar(128) primary key,
                        reason_title varchar(256) not null,
                        reason_description clob not null,
                        severity varchar(32) not null,
                        category varchar(64) not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table risk_score_result (
                        score_id bigint generated by default as identity primary key,
                        account_id varchar(64) not null,
                        risk_score double precision not null,
                        risk_level varchar(32) not null,
                        profile_name varchar(64) not null,
                        feature_version int not null,
                        generated_at timestamp not null,
                        evaluation_mode varchar(32) not null,
                        top_reason_codes clob,
                        score_breakdown_json clob not null
                    )
                    """);
        }
    }
}
