"""Shared fraud injection primitives."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import AccountRecord, DeviceRecord, IpRecord, PaymentMethodRecord
from generator.time_utils import DateRange


@dataclass(frozen=True)
class ScenarioTarget:
    account: AccountRecord
    overlap_index: int = 0


@dataclass(frozen=True)
class ScenarioLabel:
    account_id: str
    scenario_name: str
    is_fraud: bool
    confidence: float
    reason: str
    overlap_rank: int


@dataclass(frozen=True)
class ScenarioGraphLink:
    account_id: str
    device_id: str
    ip_id: str
    link_type: str
    scenario_name: str


@dataclass
class FraudInjectionResult:
    rows: dict[str, list[dict[str, object]]]
    graph_links: list[ScenarioGraphLink]
    labels: list[ScenarioLabel]

    @classmethod
    def empty(cls) -> "FraudInjectionResult":
        return cls(
            rows={
                "login_logs": [],
                "login_sessions": [],
                "transactions": [],
                "deposits": [],
                "withdrawals": [],
                "transfers": [],
                "security_events": [],
                "fraud_scenarios": [],
                "scenario_events": [],
            },
            graph_links=[],
            labels=[],
        )

    def extend(self, other: "FraudInjectionResult") -> None:
        for key, values in other.rows.items():
            self.rows.setdefault(key, []).extend(values)
        self.graph_links.extend(other.graph_links)
        self.labels.extend(other.labels)


@dataclass(frozen=True)
class ScenarioContext:
    rng: Random
    date_range: DateRange
    devices: list[DeviceRecord]
    ips: list[IpRecord]
    payment_methods: list[PaymentMethodRecord]
    settings: dict[str, float]


class FraudScenarioInjector:
    """Base class for deterministic fraud scenario injectors."""

    scenario_name = "base"

    def select_targets(
        self,
        rng: Random,
        accounts: list[AccountRecord],
        target_count: int,
        allocated_counts: dict[str, int],
    ) -> list[ScenarioTarget]:
        raise NotImplementedError

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        raise NotImplementedError


def choose_device_and_ip(
    rng: Random,
    devices: list[DeviceRecord],
    ips: list[IpRecord],
    prefer_risky_ip: bool = False,
) -> tuple[DeviceRecord, IpRecord]:
    if not devices or not ips:
        raise ValueError("devices and ips must not be empty.")
    device = devices[rng.randrange(len(devices))]
    if prefer_risky_ip:
        risky_ips = [item for item in ips if item.risk_level in {"medium", "high"}]
        ip = risky_ips[rng.randrange(len(risky_ips))] if risky_ips else ips[rng.randrange(len(ips))]
    else:
        ip = ips[rng.randrange(len(ips))]
    return device, ip


def account_payment_methods(
    payment_methods: list[PaymentMethodRecord],
    account_id: str,
) -> list[PaymentMethodRecord]:
    matched = [item for item in payment_methods if item.account_id == account_id]
    return matched or payment_methods
