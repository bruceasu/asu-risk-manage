package me.asu.ta.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.repository.RuleConfigReloadLogRepository;
import me.asu.ta.rule.repository.RuleDefinitionRepository;
import me.asu.ta.rule.repository.RuleHitLogRepository;
import me.asu.ta.rule.repository.RuleVersionRepository;
import me.asu.ta.rule.service.RuleConfigService;
import me.asu.ta.rule.service.RuleParameterParser;
import me.asu.ta.rule.service.RuleVersionService;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public final class RuleEngineCoreTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-14T12:00:00Z");

    private RuleEngineCoreTestSupport() {
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

    public static RuleDefinitionRepository ruleDefinitionRepository(DataSource dataSource) {
        return new RuleDefinitionRepository(jdbcTemplate(dataSource));
    }

    public static RuleVersionRepository ruleVersionRepository(DataSource dataSource) {
        return new RuleVersionRepository(jdbcTemplate(dataSource));
    }

    public static RuleHitLogRepository ruleHitLogRepository(DataSource dataSource) {
        return new RuleHitLogRepository(jdbcTemplate(dataSource));
    }

    public static RuleConfigReloadLogRepository ruleConfigReloadLogRepository(DataSource dataSource) {
        return new RuleConfigReloadLogRepository(jdbcTemplate(dataSource));
    }

    public static RuleConfigService ruleConfigService(DataSource dataSource) {
        RuleDefinitionRepository ruleDefinitionRepository = ruleDefinitionRepository(dataSource);
        RuleVersionService ruleVersionService = new RuleVersionService(ruleVersionRepository(dataSource));
        RuleConfigReloadLogRepository reloadLogRepository = ruleConfigReloadLogRepository(dataSource);
        return new RuleConfigService(
                ruleDefinitionRepository,
                ruleVersionService,
                reloadLogRepository,
                new RuleParameterParser(new ObjectMapper()));
    }

    public static void insertRuleDefinition(DataSource dataSource, RuleDefinition definition) {
        jdbcTemplate(dataSource).update("""
                insert into rule_definition(
                    rule_code, rule_name, category, description, severity,
                    owner_module, current_version, is_active, created_at, updated_at
                ) values (
                    :ruleCode, :ruleName, :category, :description, :severity,
                    :ownerModule, :currentVersion, :active, :createdAt, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("ruleCode", definition.ruleCode())
                .addValue("ruleName", definition.ruleName())
                .addValue("category", definition.category().name())
                .addValue("description", definition.description())
                .addValue("severity", definition.severity().name())
                .addValue("ownerModule", definition.ownerModule())
                .addValue("currentVersion", definition.currentVersion())
                .addValue("active", definition.active())
                .addValue("createdAt", definition.createdAt())
                .addValue("updatedAt", definition.updatedAt()));
    }

    public static void insertRuleVersion(DataSource dataSource, RuleVersion version) {
        jdbcTemplate(dataSource).update("""
                insert into rule_version(
                    rule_code, version, parameter_json, score_weight, enabled,
                    effective_from, effective_to, created_at, created_by, change_note
                ) values (
                    :ruleCode, :version, :parameterJson, :scoreWeight, :enabled,
                    :effectiveFrom, :effectiveTo, :createdAt, :createdBy, :changeNote
                )
                """, new MapSqlParameterSource()
                .addValue("ruleCode", version.ruleCode())
                .addValue("version", version.version())
                .addValue("parameterJson", version.parameterJson())
                .addValue("scoreWeight", version.scoreWeight())
                .addValue("enabled", version.enabled())
                .addValue("effectiveFrom", version.effectiveFrom())
                .addValue("effectiveTo", version.effectiveTo())
                .addValue("createdAt", version.createdAt())
                .addValue("createdBy", version.createdBy())
                .addValue("changeNote", version.changeNote()));
    }

    public static AccountFeatureSnapshot.Builder snapshotBuilder(String accountId) {
        return AccountFeatureSnapshot.builder(accountId, FIXED_TIME)
                .featureVersion(1)
                .accountAgeDays(200)
                .kycLevelNumeric(2)
                .registrationIpRiskScore(0.10d)
                .loginCount24h(6)
                .loginFailureCount24h(1)
                .loginFailureRate24h(0.16d)
                .uniqueIpCount24h(2)
                .highRiskIpLoginCount24h(0)
                .vpnIpLoginCount24h(0)
                .newDeviceLoginCount7d(0)
                .nightLoginRatio7d(0.05d)
                .transactionCount24h(3)
                .totalAmount24h(500.0d)
                .avgTransactionAmount24h(166.7d)
                .depositCount24h(1)
                .withdrawCount24h(1)
                .depositAmount24h(300.0d)
                .withdrawAmount24h(80.0d)
                .depositWithdrawRatio24h(0.26d)
                .uniqueCounterpartyCount24h(2)
                .withdrawAfterDepositDelayAvg24h(180.0d)
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
                .riskNeighborCount30d(0);
    }

    private static void initializeSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table rule_definition (
                        rule_code varchar(128) primary key,
                        rule_name varchar(256) not null,
                        category varchar(64) not null,
                        description clob not null,
                        severity varchar(32) not null,
                        owner_module varchar(64) not null,
                        current_version int not null,
                        is_active boolean not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            statement.execute("""
                    create table rule_version (
                        rule_code varchar(128) not null,
                        version int not null,
                        parameter_json clob not null,
                        score_weight int not null,
                        enabled boolean not null,
                        effective_from timestamp not null,
                        effective_to timestamp,
                        created_at timestamp not null,
                        created_by varchar(128),
                        change_note clob,
                        primary key (rule_code, version)
                    )
                    """);
            statement.execute("""
                    create table rule_hit_log (
                        hit_id bigint generated by default as identity primary key,
                        account_id varchar(64) not null,
                        rule_code varchar(128) not null,
                        rule_version int not null,
                        hit_time timestamp not null,
                        score int not null,
                        reason_code varchar(128) not null,
                        evidence_json clob,
                        feature_version int not null,
                        evaluation_mode varchar(32) not null
                    )
                    """);
            statement.execute("""
                    create table rule_config_reload_log (
                        reload_id bigint generated by default as identity primary key,
                        reload_time timestamp not null,
                        status varchar(32) not null,
                        loaded_rule_count int,
                        error_message clob
                    )
                    """);
        }
    }
}
