package me.asu.ta.feature.definition;

import java.time.Instant;
import java.util.List;
import me.asu.ta.feature.model.FeatureDefinition;

public final class FeatureDefinitionCatalog {

    private FeatureDefinitionCatalog() {
    }

    public static List<FeatureDefinition> defaults() {
        return List.of(
                definition(FeatureName.ACCOUNT_AGE_DAYS, "INT", "account_base", "Account age in days", "accounts", "all"),
                definition(FeatureName.KYC_LEVEL_NUMERIC, "INT", "account_base", "Numeric KYC level", "account_profiles", "all"),
                definition(FeatureName.LOGIN_COUNT_24H, "INT", "login_behavior", "Successful and failed logins in 24 hours", "login_events", "24h"),
                definition(FeatureName.LOGIN_FAILURE_COUNT_24H, "INT", "login_behavior", "Failed logins in 24 hours", "login_events", "24h"),
                definition(FeatureName.LOGIN_FAILURE_RATE_24H, "DOUBLE", "login_behavior", "Failed login ratio in 24 hours", "login_events", "24h"),
                definition(FeatureName.UNIQUE_IP_COUNT_24H, "INT", "login_behavior", "Distinct login IP count", "login_events", "24h"),
                definition(FeatureName.HIGH_RISK_IP_LOGIN_COUNT_24H, "INT", "login_behavior", "High risk IP login count", "login_events,ips", "24h"),
                definition(FeatureName.TRANSACTION_COUNT_24H, "INT", "transaction_behavior", "Transaction count in 24 hours", "card_transactions", "24h"),
                definition(FeatureName.TOTAL_AMOUNT_24H, "DOUBLE", "transaction_behavior", "Total card transaction amount in 24 hours", "card_transactions", "24h"),
                definition(FeatureName.AVG_TRANSACTION_AMOUNT_24H, "DOUBLE", "transaction_behavior", "Average transaction amount in 24 hours", "card_transactions", "24h"),
                definition(FeatureName.DEPOSIT_COUNT_24H, "INT", "transaction_behavior", "Deposit count in 24 hours", "payment_attempts", "24h"),
                definition(FeatureName.WITHDRAW_COUNT_24H, "INT", "transaction_behavior", "Withdrawal count in 24 hours", "cash_out_events", "24h"),
                definition(FeatureName.DEPOSIT_AMOUNT_24H, "DOUBLE", "transaction_behavior", "Deposit amount in 24 hours", "payment_attempts", "24h"),
                definition(FeatureName.WITHDRAW_AMOUNT_24H, "DOUBLE", "transaction_behavior", "Withdrawal amount in 24 hours", "cash_out_events", "24h"),
                definition(FeatureName.DEPOSIT_WITHDRAW_RATIO_24H, "DOUBLE", "transaction_behavior", "Withdrawal to deposit ratio", "payment_attempts,cash_out_events", "24h"),
                definition(FeatureName.UNIQUE_COUNTERPARTY_COUNT_24H, "INT", "transaction_behavior", "Distinct transfer counterparties", "p2p_transfers", "24h"),
                definition(FeatureName.RAPID_WITHDRAW_AFTER_DEPOSIT_FLAG_24H, "BOOLEAN", "transaction_behavior", "Deposit followed by quick cash out", "payment_attempts,cash_out_events", "24h"),
                definition(FeatureName.UNIQUE_DEVICE_COUNT_7D, "INT", "device_behavior", "Distinct devices in 7 days", "login_events", "7d"),
                definition(FeatureName.DEVICE_SWITCH_COUNT_24H, "INT", "device_behavior", "Device switches in 24 hours", "login_events", "24h"),
                definition(FeatureName.SHARED_DEVICE_ACCOUNTS_7D, "INT", "device_behavior", "Other accounts sharing device", "account_device_links", "7d"),
                definition(FeatureName.SUSPICIOUS_DEVICE_REUSE_FLAG_30D, "BOOLEAN", "device_behavior", "Shared device account count over threshold", "account_device_links", "30d"),
                definition(FeatureName.SECURITY_EVENT_COUNT_24H, "INT", "security_behavior", "Security events in 24 hours", "alerts,cases", "24h"),
                definition(FeatureName.PASSWORD_CHANGE_COUNT_7D, "INT", "security_behavior", "Password related security changes", "cases", "7d"),
                definition(FeatureName.RAPID_PROFILE_CHANGE_FLAG_24H, "BOOLEAN", "security_behavior", "Burst of security changes in 24 hours", "cases", "24h"),
                definition(FeatureName.SECURITY_CHANGE_BEFORE_WITHDRAW_FLAG_24H, "BOOLEAN", "security_behavior", "Security event before withdrawal", "alerts,cash_out_events", "24h"),
                definition(FeatureName.SHARED_IP_ACCOUNTS_7D, "INT", "graph_features", "Accounts sharing IP", "account_ip_links", "7d"),
                definition(FeatureName.SHARED_BANK_ACCOUNTS_30D, "INT", "graph_features", "Accounts sharing beneficiary/bank", "bank_transfers", "30d"),
                definition(FeatureName.GRAPH_CLUSTER_SIZE_30D, "INT", "graph_features", "Approximate connected component size", "account_device_links,account_ip_links", "30d"),
                definition(FeatureName.RISK_NEIGHBOR_COUNT_30D, "INT", "graph_features", "Neighbors with fraud labels", "fraud_labels", "30d"),
                definition(FeatureName.ANOMALY_SCORE_LAST, "DOUBLE", "statistical_anomaly", "Latest anomaly score", "scenario_events", "24h")
        );
    }

    private static FeatureDefinition definition(
            FeatureName name,
            String dataType,
            String category,
            String definition,
            String sourceTables,
            String windowSpec) {
        Instant now = Instant.now();
        return new FeatureDefinition(
                name.name().toLowerCase(),
                "ACCOUNT",
                dataType,
                category,
                definition,
                windowSpec,
                null,
                "BATCH_SQL",
                sourceTables,
                "fraud-feature",
                1,
                true,
                true,
                true,
                true,
                now,
                now
        );
    }
}
