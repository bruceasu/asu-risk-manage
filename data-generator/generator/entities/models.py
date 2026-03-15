"""Core entity dataclasses used during generation."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AccountRecord:
    account_id: str
    customer_id: str
    country: str
    currency: str
    status: str
    risk_band: str
    fraud_segment: str
    base_balance: float
    kyc_level: str = "standard"
    account_type: str = "personal"
    created_at: str = ""
    cluster_id: str = ""


@dataclass(frozen=True)
class DeviceRecord:
    device_id: str
    platform: str
    trust_score: int
    device_type: str = "mobile"
    operating_system: str = "android"
    created_at: str = ""


@dataclass(frozen=True)
class IpRecord:
    ip_id: str
    ip_address: str
    country: str
    risk_score: int
    risk_level: str = "low"
    created_at: str = ""


@dataclass(frozen=True)
class BankRecord:
    bank_id: str
    bank_name: str
    country: str
    routing_code: str
    swift_code: str
    created_at: str


@dataclass(frozen=True)
class PaymentMethodRecord:
    payment_method_id: str
    account_id: str
    cluster_id: str
    method_type: str
    issuing_bank_id: str
    currency: str
    created_at: str
