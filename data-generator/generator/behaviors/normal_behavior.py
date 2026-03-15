"""Coordinated normal behavior generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.behaviors.graph_behavior import (
    AccountGraphProfile,
    build_account_graph_profile,
    get_segment_behavior,
)
from generator.behaviors.login_behavior import LoginBehaviorGenerator
from generator.behaviors.security_behavior import SecurityBehaviorGenerator
from generator.behaviors.transaction_behavior import TransactionBehaviorGenerator
from generator.entities.models import AccountRecord, DeviceRecord, IpRecord, PaymentMethodRecord
from generator.time_utils import DateRange
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class NormalBehaviorConfig:
    segment_weights: dict[str, float]


class NormalBehaviorGenerator:
    """Coordinates normal behavior generation across behavior modules."""

    def __init__(self, rng: Random, date_range: DateRange, config: NormalBehaviorConfig) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.segment_chooser = WeightedChooser(config.segment_weights)
        self.login_generator = LoginBehaviorGenerator(rng, date_range)
        self.transaction_generator = TransactionBehaviorGenerator(rng, date_range)
        self.security_generator = SecurityBehaviorGenerator(rng, date_range)

    def assign_segments(self, accounts: list[AccountRecord]) -> dict[str, str]:
        assignments: dict[str, str] = {}
        for account in accounts:
            assignments[account.account_id] = self.segment_chooser.choose(self.rng)
        return assignments

    def generate_for_accounts(
        self,
        accounts: list[AccountRecord],
        devices: list[DeviceRecord],
        ips: list[IpRecord],
        payment_methods: list[PaymentMethodRecord],
        segment_assignments: dict[str, str] | None = None,
    ) -> dict[str, list[dict[str, object]]]:
        assignments = segment_assignments or self.assign_segments(accounts)
        rows: dict[str, list[dict[str, object]]] = {
            "login_logs": [],
            "login_sessions": [],
            "transactions": [],
            "deposits": [],
            "withdrawals": [],
            "transfers": [],
            "security_events": [],
        }
        for account in accounts:
            segment_name = assignments[account.account_id]
            get_segment_behavior(segment_name)
            graph_profile = build_account_graph_profile(self.rng, account, devices, ips, segment_name)
            self._append_account_rows(rows, account, graph_profile, segment_name, payment_methods)
        return rows

    def _append_account_rows(
        self,
        rows: dict[str, list[dict[str, object]]],
        account: AccountRecord,
        graph_profile: AccountGraphProfile,
        segment_name: str,
        payment_methods: list[PaymentMethodRecord],
    ) -> None:
        login_rows = self.login_generator.generate(account, graph_profile, segment_name)
        transaction_rows = self.transaction_generator.generate(account, graph_profile, payment_methods, segment_name)
        security_rows = self.security_generator.generate(account, graph_profile, segment_name)
        rows["login_logs"].extend(login_rows.login_logs)
        rows["login_sessions"].extend(login_rows.login_sessions)
        rows["transactions"].extend(transaction_rows.transactions)
        rows["deposits"].extend(transaction_rows.deposits)
        rows["withdrawals"].extend(transaction_rows.withdrawals)
        rows["transfers"].extend(transaction_rows.transfers)
        rows["security_events"].extend(security_rows.security_events)
