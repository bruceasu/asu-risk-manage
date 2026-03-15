"""Canonical CSV schema metadata."""

from __future__ import annotations


CSV_SCHEMAS: dict[str, list[str]] = {
    "accounts.csv": ["account_id", "customer_id", "country", "currency", "status", "risk_band", "created_at"],
    "account_profiles.csv": ["account_id", "segment", "kyc_level", "email_domain", "phone_country_code"],
    "account_balances.csv": ["account_id", "available_balance", "ledger_balance", "balance_updated_at"],
    "devices.csv": ["device_id", "platform", "trust_score", "first_seen_at"],
    "ips.csv": ["ip_id", "ip_address", "country", "risk_score", "first_seen_at"],
    "merchants.csv": ["merchant_id", "merchant_name", "merchant_category", "country"],
    "beneficiaries.csv": ["beneficiary_id", "beneficiary_name", "country", "bank_code"],
    "login_events.csv": ["event_id", "account_id", "device_id", "ip_id", "event_time", "success", "risk_score"],
    "session_events.csv": ["event_id", "account_id", "session_id", "device_id", "event_time", "duration_seconds"],
    "page_views.csv": ["event_id", "account_id", "session_id", "page_name", "event_time", "dwell_ms"],
    "payment_attempts.csv": ["event_id", "account_id", "merchant_id", "amount", "currency", "event_time", "decision"],
    "card_transactions.csv": ["event_id", "account_id", "merchant_id", "card_last4", "amount", "currency", "event_time"],
    "bank_transfers.csv": ["event_id", "account_id", "beneficiary_id", "amount", "currency", "event_time", "direction"],
    "cash_out_events.csv": ["event_id", "account_id", "amount", "currency", "event_time", "channel"],
    "p2p_transfers.csv": ["event_id", "account_id", "counterparty_account_id", "amount", "currency", "event_time"],
    "chargebacks.csv": ["event_id", "account_id", "original_event_id", "amount", "currency", "event_time", "reason_code"],
    "alerts.csv": ["alert_id", "account_id", "rule_name", "severity", "event_time", "status"],
    "cases.csv": ["case_id", "account_id", "alert_id", "case_type", "opened_at", "status"],
    "account_device_links.csv": ["account_id", "device_id", "linked_at", "is_primary"],
    "account_ip_links.csv": ["account_id", "ip_id", "linked_at", "usage_count"],
    "device_ip_links.csv": ["device_id", "ip_id", "linked_at", "confidence"],
    "fraud_labels.csv": ["account_id", "is_fraud", "scenario_name", "label_reason", "labeled_at"],
    "fraud_scenarios.csv": ["scenario_id", "account_id", "scenario_name", "intensity", "created_at"],
    "scenario_events.csv": ["event_id", "account_id", "scenario_name", "event_time", "signal", "score"],
    "generation_metrics.csv": ["metric_name", "metric_value"],
}


def columns_for(csv_name: str) -> list[str]:
    if csv_name not in CSV_SCHEMAS:
        raise KeyError(f"Unknown CSV schema: {csv_name}")
    return CSV_SCHEMAS[csv_name]
