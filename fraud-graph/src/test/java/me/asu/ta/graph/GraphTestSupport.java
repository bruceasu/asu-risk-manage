package me.asu.ta.graph;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import me.asu.ta.graph.repository.AccountGraphSignalRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public final class GraphTestSupport {
    public static final Instant FIXED_TIME = Instant.parse("2026-03-15T00:00:00Z");
    public static final Instant WINDOW_START = FIXED_TIME.minusSeconds(86_400);
    public static final Instant WINDOW_END = FIXED_TIME;

    private GraphTestSupport() {
    }

    public static DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:graph_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplate(dataSource);
        createSourceTables(jdbcTemplate);
        createGraphSignalTable(jdbcTemplate);
        return dataSource;
    }

    public static NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    public static AccountGraphSignalRepository accountGraphSignalRepository(DataSource dataSource) {
        return new AccountGraphSignalRepository(jdbcTemplate(dataSource));
    }

    public static void insertAccountDevice(DataSource dataSource, String accountId, String deviceId, Instant linkedAt) {
        jdbcTemplate(dataSource).update("""
                insert into account_devices(account_id, device_id, linked_at)
                values (:accountId, :deviceId, :linkedAt)
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("deviceId", deviceId)
                .addValue("linkedAt", Timestamp.from(linkedAt)));
    }

    public static void insertLoginLog(DataSource dataSource, String accountId, String ipAddress, String ipRiskLevel, Instant loginTime) {
        jdbcTemplate(dataSource).update("""
                insert into login_logs(account_id, ip_address, ip_risk_level, login_time)
                values (:accountId, :ipAddress, :ipRiskLevel, :loginTime)
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("ipAddress", ipAddress)
                .addValue("ipRiskLevel", ipRiskLevel)
                .addValue("loginTime", Timestamp.from(loginTime)));
    }

    public static void insertBankAccount(DataSource dataSource, String accountId, String bankAccountId, Instant linkedAt) {
        jdbcTemplate(dataSource).update("""
                insert into bank_accounts(account_id, bank_account_id, linked_at)
                values (:accountId, :bankAccountId, :linkedAt)
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("bankAccountId", bankAccountId)
                .addValue("linkedAt", Timestamp.from(linkedAt)));
    }

    public static void insertTransfer(DataSource dataSource, String fromAccountId, String toAccountId, double amount, Instant createdAt) {
        jdbcTemplate(dataSource).update("""
                insert into transfers(from_account_id, to_account_id, amount, created_at)
                values (:fromAccountId, :toAccountId, :amount, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("fromAccountId", fromAccountId)
                .addValue("toAccountId", toAccountId)
                .addValue("amount", amount)
                .addValue("createdAt", Timestamp.from(createdAt)));
    }

    public static me.asu.ta.graph.model.AccountGraphSignal sampleSignal(String accountId) {
        return new me.asu.ta.graph.model.AccountGraphSignal(
                accountId,
                WINDOW_START,
                WINDOW_END,
                44.7d,
                7,
                2,
                1,
                1,
                1,
                1,
                true,
                3,
                1,
                100.0d,
                72.0d,
                FIXED_TIME);
    }

    public static void insertAccountGraphSignal(DataSource dataSource, me.asu.ta.graph.model.AccountGraphSignal signal) {
        jdbcTemplate(dataSource).update("""
                insert into account_graph_signal(
                    account_id, graph_window_start, graph_window_end, graph_score, graph_cluster_size,
                    risk_neighbor_count, two_hop_risk_neighbor_count, shared_device_accounts,
                    shared_ip_accounts, shared_bank_accounts, collector_account_flag,
                    funnel_in_degree, funnel_out_degree, local_density_score, cluster_risk_score, generated_at
                ) values (
                    :accountId, :graphWindowStart, :graphWindowEnd, :graphScore, :graphClusterSize,
                    :riskNeighborCount, :twoHopRiskNeighborCount, :sharedDeviceAccounts,
                    :sharedIpAccounts, :sharedBankAccounts, :collectorAccountFlag,
                    :funnelInDegree, :funnelOutDegree, :localDensityScore, :clusterRiskScore, :generatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("accountId", signal.accountId())
                .addValue("graphWindowStart", Timestamp.from(signal.graphWindowStart()))
                .addValue("graphWindowEnd", Timestamp.from(signal.graphWindowEnd()))
                .addValue("graphScore", signal.graphScore())
                .addValue("graphClusterSize", signal.graphClusterSize())
                .addValue("riskNeighborCount", signal.riskNeighborCount())
                .addValue("twoHopRiskNeighborCount", signal.twoHopRiskNeighborCount())
                .addValue("sharedDeviceAccounts", signal.sharedDeviceAccounts())
                .addValue("sharedIpAccounts", signal.sharedIpAccounts())
                .addValue("sharedBankAccounts", signal.sharedBankAccounts())
                .addValue("collectorAccountFlag", signal.collectorAccountFlag())
                .addValue("funnelInDegree", signal.funnelInDegree())
                .addValue("funnelOutDegree", signal.funnelOutDegree())
                .addValue("localDensityScore", signal.localDensityScore())
                .addValue("clusterRiskScore", signal.clusterRiskScore())
                .addValue("generatedAt", Timestamp.from(signal.generatedAt())));
    }

    public static void insertSharedIpHotspot(DataSource dataSource, String ipAddress, int accountCount) {
        for (int i = 0; i < accountCount; i++) {
            insertLoginLog(dataSource, "acct-hot-" + i, ipAddress, "MEDIUM", WINDOW_START.plusSeconds(600 + i));
        }
    }

    public static void insertSharedBankHotspot(DataSource dataSource, String bankAccountId, int accountCount) {
        for (int i = 0; i < accountCount; i++) {
            insertBankAccount(dataSource, "acct-bank-hot-" + i, bankAccountId, WINDOW_START.plusSeconds(900 + i));
        }
    }

    public static Map<String, Integer> asMap(List<String> accountIds, int value) {
        return accountIds.stream().collect(java.util.stream.Collectors.toMap(accountId -> accountId, accountId -> value));
    }

    private static void createSourceTables(NamedParameterJdbcTemplate jdbcTemplate) {
        jdbcTemplate.getJdbcTemplate().execute("""
                create table account_devices (
                    account_id varchar(64) not null,
                    device_id varchar(128) not null,
                    linked_at timestamp not null
                )
                """);
        jdbcTemplate.getJdbcTemplate().execute("""
                create table login_logs (
                    account_id varchar(64) not null,
                    ip_address varchar(128) not null,
                    ip_risk_level varchar(32),
                    login_time timestamp not null
                )
                """);
        jdbcTemplate.getJdbcTemplate().execute("""
                create table bank_accounts (
                    account_id varchar(64) not null,
                    bank_account_id varchar(128) not null,
                    linked_at timestamp not null
                )
                """);
        jdbcTemplate.getJdbcTemplate().execute("""
                create table transfers (
                    from_account_id varchar(64) not null,
                    to_account_id varchar(64) not null,
                    amount double precision not null,
                    created_at timestamp not null
                )
                """);
    }

    private static void createGraphSignalTable(NamedParameterJdbcTemplate jdbcTemplate) {
        jdbcTemplate.getJdbcTemplate().execute("""
                create table account_graph_signal (
                    account_id varchar(64) primary key,
                    graph_window_start timestamp not null,
                    graph_window_end timestamp not null,
                    graph_score double precision not null,
                    graph_cluster_size integer,
                    risk_neighbor_count integer,
                    two_hop_risk_neighbor_count integer,
                    shared_device_accounts integer,
                    shared_ip_accounts integer,
                    shared_bank_accounts integer,
                    collector_account_flag boolean,
                    funnel_in_degree integer,
                    funnel_out_degree integer,
                    local_density_score double precision,
                    cluster_risk_score double precision,
                    generated_at timestamp not null
                )
                """);
    }
}
