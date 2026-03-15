"""Lightweight CSV row validation helpers."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal, InvalidOperation


class SchemaValidationError(ValueError):
    """Raised when a generated CSV row violates schema rules."""


@dataclass(frozen=True)
class RangeRule:
    minimum: float | None = None
    maximum: float | None = None


@dataclass(frozen=True)
class TimestampOrderRule:
    earlier_field: str
    later_field: str


@dataclass(frozen=True)
class FileValidationRule:
    required_fields: tuple[str, ...] = ()
    enum_fields: dict[str, frozenset[str]] | None = None
    numeric_ranges: dict[str, RangeRule] | None = None
    timestamp_order: tuple[TimestampOrderRule, ...] = ()


FILE_VALIDATIONS: dict[str, FileValidationRule] = {
    "accounts.csv": FileValidationRule(
        required_fields=("account_id", "customer_id", "country", "currency", "status", "risk_band", "created_at"),
        enum_fields={
            "status": frozenset({"active", "review", "suspended", "closed"}),
            "risk_band": frozenset({"low", "medium", "high"}),
        },
    ),
    "account_profiles.csv": FileValidationRule(
        required_fields=("account_id", "segment", "kyc_level", "email_domain", "phone_country_code"),
        enum_fields={
            "kyc_level": frozenset({"standard", "enhanced", "simplified"}),
        },
    ),
    "account_balances.csv": FileValidationRule(
        required_fields=("account_id", "available_balance", "ledger_balance", "balance_updated_at"),
        numeric_ranges={
            "available_balance": RangeRule(minimum=0.0),
            "ledger_balance": RangeRule(minimum=-1000.0),
        },
    ),
    "devices.csv": FileValidationRule(
        required_fields=("device_id", "platform", "trust_score", "first_seen_at"),
        enum_fields={"platform": frozenset({"ios", "android", "web", "desktop", "app"})},
        numeric_ranges={"trust_score": RangeRule(minimum=0, maximum=100)},
    ),
    "ips.csv": FileValidationRule(
        required_fields=("ip_id", "ip_address", "country", "risk_score", "first_seen_at"),
        numeric_ranges={"risk_score": RangeRule(minimum=0, maximum=100)},
    ),
    "login_events.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "device_id", "ip_id", "event_time", "success", "risk_score"),
        enum_fields={"success": frozenset({"true", "false"})},
        numeric_ranges={"risk_score": RangeRule(minimum=0, maximum=100)},
    ),
    "session_events.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "session_id", "device_id", "event_time", "duration_seconds"),
        numeric_ranges={"duration_seconds": RangeRule(minimum=0, maximum=86400)},
    ),
    "page_views.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "session_id", "page_name", "event_time", "dwell_ms"),
        numeric_ranges={"dwell_ms": RangeRule(minimum=0, maximum=600000)},
    ),
    "payment_attempts.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "merchant_id", "amount", "currency", "event_time", "decision"),
        enum_fields={"decision": frozenset({"approve", "challenge", "decline"})},
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "card_transactions.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "merchant_id", "card_last4", "amount", "currency", "event_time"),
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "bank_transfers.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "beneficiary_id", "amount", "currency", "event_time", "direction"),
        enum_fields={"direction": frozenset({"outbound", "inbound"})},
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "cash_out_events.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "amount", "currency", "event_time", "channel"),
        enum_fields={"channel": frozenset({"atm", "branch", "crypto_offramp"})},
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "p2p_transfers.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "counterparty_account_id", "amount", "currency", "event_time"),
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "chargebacks.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "original_event_id", "amount", "currency", "event_time", "reason_code"),
        enum_fields={"reason_code": frozenset({"fraud", "dispute", "processing_error"})},
        numeric_ranges={"amount": RangeRule(minimum=0.0)},
    ),
    "alerts.csv": FileValidationRule(
        required_fields=("alert_id", "account_id", "rule_name", "severity", "event_time", "status"),
        enum_fields={
            "severity": frozenset({"low", "medium", "high"}),
            "status": frozenset({"open", "triaged", "closed"}),
        },
    ),
    "cases.csv": FileValidationRule(
        required_fields=("case_id", "account_id", "alert_id", "case_type", "opened_at", "status"),
        enum_fields={"status": frozenset({"new", "investigating", "closed"})},
    ),
    "account_device_links.csv": FileValidationRule(
        required_fields=("account_id", "device_id", "linked_at", "is_primary"),
        enum_fields={"is_primary": frozenset({"true", "false"})},
    ),
    "account_ip_links.csv": FileValidationRule(
        required_fields=("account_id", "ip_id", "linked_at", "usage_count"),
        numeric_ranges={"usage_count": RangeRule(minimum=0)},
    ),
    "device_ip_links.csv": FileValidationRule(
        required_fields=("device_id", "ip_id", "linked_at", "confidence"),
        numeric_ranges={"confidence": RangeRule(minimum=0.0, maximum=1.0)},
    ),
    "fraud_labels.csv": FileValidationRule(
        required_fields=("account_id", "is_fraud", "label_reason", "labeled_at"),
        enum_fields={"is_fraud": frozenset({"true", "false"})},
    ),
    "fraud_scenarios.csv": FileValidationRule(
        required_fields=("scenario_id", "account_id", "scenario_name", "intensity", "created_at"),
        numeric_ranges={"intensity": RangeRule(minimum=0.0, maximum=1.0)},
    ),
    "scenario_events.csv": FileValidationRule(
        required_fields=("event_id", "account_id", "scenario_name", "event_time", "signal", "score"),
        numeric_ranges={"score": RangeRule(minimum=0.0, maximum=1.0)},
    ),
    "generation_metrics.csv": FileValidationRule(
        required_fields=("metric_name", "metric_value"),
    ),
}


def validate_row(csv_name: str, row: dict[str, object]) -> None:
    rule = FILE_VALIDATIONS.get(csv_name)
    if rule is None:
        return
    _validate_required(csv_name, row, rule.required_fields)
    _validate_enums(csv_name, row, rule.enum_fields or {})
    _validate_numeric_ranges(csv_name, row, rule.numeric_ranges or {})
    _validate_timestamp_order(csv_name, row, rule.timestamp_order)
    _validate_file_specific_rules(csv_name, row)


def _validate_required(csv_name: str, row: dict[str, object], required_fields: tuple[str, ...]) -> None:
    for field in required_fields:
        value = row.get(field)
        if value is None or value == "":
            raise SchemaValidationError(f"{csv_name}: missing required field '{field}'")


def _validate_enums(csv_name: str, row: dict[str, object], enum_fields: dict[str, frozenset[str]]) -> None:
    for field, allowed in enum_fields.items():
        value = row.get(field)
        if value is None:
            continue
        if str(value) not in allowed:
            raise SchemaValidationError(f"{csv_name}: invalid enum value for '{field}': {value!r}")


def _validate_numeric_ranges(csv_name: str, row: dict[str, object], numeric_ranges: dict[str, RangeRule]) -> None:
    for field, range_rule in numeric_ranges.items():
        value = row.get(field)
        if value is None or value == "":
            continue
        number = _to_decimal(csv_name, field, value)
        if range_rule.minimum is not None and number < Decimal(str(range_rule.minimum)):
            raise SchemaValidationError(f"{csv_name}: field '{field}' below minimum {range_rule.minimum}")
        if range_rule.maximum is not None and number > Decimal(str(range_rule.maximum)):
            raise SchemaValidationError(f"{csv_name}: field '{field}' above maximum {range_rule.maximum}")


def _validate_timestamp_order(
    csv_name: str,
    row: dict[str, object],
    timestamp_rules: tuple[TimestampOrderRule, ...],
) -> None:
    for rule in timestamp_rules:
        earlier = row.get(rule.earlier_field)
        later = row.get(rule.later_field)
        if earlier in (None, "") or later in (None, ""):
            continue
        earlier_dt = _to_datetime(csv_name, rule.earlier_field, earlier)
        later_dt = _to_datetime(csv_name, rule.later_field, later)
        if earlier_dt > later_dt:
            raise SchemaValidationError(
                f"{csv_name}: timestamp ordering violated for '{rule.earlier_field}' <= '{rule.later_field}'"
            )


def _to_decimal(csv_name: str, field: str, value: object) -> Decimal:
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError) as exc:
        raise SchemaValidationError(f"{csv_name}: field '{field}' is not numeric: {value!r}") from exc


def _to_datetime(csv_name: str, field: str, value: object) -> datetime:
    try:
        return datetime.fromisoformat(str(value))
    except ValueError as exc:
        raise SchemaValidationError(f"{csv_name}: field '{field}' is not a valid ISO timestamp: {value!r}") from exc


def _validate_file_specific_rules(csv_name: str, row: dict[str, object]) -> None:
    if csv_name == "fraud_labels.csv":
        is_fraud = str(row.get("is_fraud", "false"))
        scenario_name = str(row.get("scenario_name", ""))
        if is_fraud == "true" and not scenario_name:
            raise SchemaValidationError("fraud_labels.csv: scenario_name is required when is_fraud is true")
