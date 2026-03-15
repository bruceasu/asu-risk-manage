"""Transaction-oriented normal behavior generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.behaviors.graph_behavior import AccountGraphProfile, get_segment_behavior
from generator.behaviors.login_behavior import _natural_activity_time
from generator.entities.models import AccountRecord, PaymentMethodRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp


@dataclass(frozen=True)
class TransactionBehaviorResult:
    transactions: list[dict[str, object]]
    deposits: list[dict[str, object]]
    withdrawals: list[dict[str, object]]
    transfers: list[dict[str, object]]


class TransactionBehaviorGenerator:
    """Generates natural-looking normal money movement rows."""

    def __init__(self, rng: Random, date_range: DateRange) -> None:
        self.rng = rng
        self.date_range = date_range
        self.transaction_ids = IdFactory("TRX", width=7, separator="")
        self.deposit_ids = IdFactory("DEP", width=7, separator="")
        self.withdrawal_ids = IdFactory("WDL", width=7, separator="")
        self.transfer_ids = IdFactory("XFR", width=7, separator="")

    def generate(
        self,
        account: AccountRecord,
        graph_profile: AccountGraphProfile,
        payment_methods: list[PaymentMethodRecord],
        segment_name: str,
    ) -> TransactionBehaviorResult:
        behavior = get_segment_behavior(segment_name)
        transaction_count = self.rng.randint(*behavior.monthly_transaction_range)
        account_methods = [item for item in payment_methods if item.account_id == account.account_id] or payment_methods
        transactions: list[dict[str, object]] = []
        deposits: list[dict[str, object]] = []
        withdrawals: list[dict[str, object]] = []
        transfers: list[dict[str, object]] = []
        for index in range(transaction_count):
            event_time = _natural_activity_time(
                self.rng,
                self.date_range,
                behavior.night_activity_weight,
                behavior.weekend_activity_weight,
            )
            payment_method = account_methods[index % len(account_methods)]
            amount = _segment_amount(self.rng, segment_name, account.base_balance)
            transaction_id = self.transaction_ids.build(index + 1)
            transactions.append(
                {
                    "transaction_id": transaction_id,
                    "account_id": account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "device_id": graph_profile.primary_device_ids[index % len(graph_profile.primary_device_ids)],
                    "ip_id": graph_profile.primary_ip_ids[index % len(graph_profile.primary_ip_ids)],
                    "transaction_time": format_timestamp(event_time),
                    "amount": f"{amount:.2f}",
                    "currency": account.currency,
                    "merchant_category": self.rng.choice(["grocery", "travel", "food", "utilities", "subscriptions"]),
                    "segment_name": segment_name,
                }
            )
            roll = self.rng.random()
            if roll < behavior.deposit_ratio:
                deposits.append(
                    {
                        "deposit_id": self.deposit_ids.build(len(deposits) + 1),
                        "account_id": account.account_id,
                        "payment_method_id": payment_method.payment_method_id,
                        "deposit_time": format_timestamp(event_time),
                        "amount": f"{max(10.0, amount * self.rng.uniform(0.8, 1.4)):.2f}",
                        "currency": account.currency,
                        "source_type": self.rng.choice(["salary", "cash_in", "refund", "merchant_settlement"]),
                        "segment_name": segment_name,
                    }
                )
            elif roll < behavior.deposit_ratio + behavior.withdrawal_ratio:
                withdrawals.append(
                    {
                        "withdrawal_id": self.withdrawal_ids.build(len(withdrawals) + 1),
                        "account_id": account.account_id,
                        "payment_method_id": payment_method.payment_method_id,
                        "withdrawal_time": format_timestamp(event_time),
                        "amount": f"{max(5.0, amount * self.rng.uniform(0.5, 1.1)):.2f}",
                        "currency": account.currency,
                        "channel": self.rng.choice(["atm", "bank_transfer", "merchant_refund"]),
                        "segment_name": segment_name,
                    }
                )
            elif roll < behavior.deposit_ratio + behavior.withdrawal_ratio + behavior.transfer_ratio:
                transfers.append(
                    {
                        "transfer_id": self.transfer_ids.build(len(transfers) + 1),
                        "account_id": account.account_id,
                        "counterparty_cluster_id": account.cluster_id,
                        "transfer_time": format_timestamp(event_time),
                        "amount": f"{max(5.0, amount * self.rng.uniform(0.7, 1.2)):.2f}",
                        "currency": account.currency,
                        "direction": self.rng.choice(["outbound", "inbound"]),
                        "segment_name": segment_name,
                    }
                )
        return TransactionBehaviorResult(
            transactions=transactions,
            deposits=deposits,
            withdrawals=withdrawals,
            transfers=transfers,
        )


def _segment_amount(rng: Random, segment_name: str, base_balance: float) -> float:
    if segment_name == "dormant_accounts":
        return rng.triangular(8.0, min(150.0, base_balance * 0.12), 25.0)
    if segment_name == "high_activity_normal":
        return rng.triangular(10.0, min(1500.0, base_balance * 0.35), 120.0)
    if segment_name == "new_users":
        return rng.triangular(5.0, min(400.0, base_balance * 0.2), 45.0)
    if segment_name == "medium_activity_normal":
        return rng.triangular(8.0, min(900.0, base_balance * 0.28), 80.0)
    return rng.triangular(5.0, min(500.0, base_balance * 0.18), 50.0)
