"""Deterministic payment method generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import AccountRecord, BankRecord, PaymentMethodRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class PaymentMethodGeneratorConfig:
    count: int
    method_type_weights: dict[str, float]
    currency_weights: dict[str, float]
    cluster_size: int = 12


class PaymentMethodGenerator:
    """Generates deterministic synthetic payment methods."""

    def __init__(
        self,
        rng: Random,
        date_range: DateRange,
        config: PaymentMethodGeneratorConfig,
    ) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.payment_method_ids = IdFactory("PM", width=7, separator="")
        self.cluster_ids = IdFactory.cluster_ids()
        self.method_type_chooser = WeightedChooser(config.method_type_weights)
        self.currency_chooser = WeightedChooser(config.currency_weights)

    def generate(
        self,
        accounts: list[AccountRecord],
        banks: list[BankRecord],
    ) -> list[PaymentMethodRecord]:
        if not accounts:
            raise ValueError("accounts must not be empty.")
        if not banks:
            raise ValueError("banks must not be empty.")
        return [self._build_payment_method(index, accounts, banks) for index in range(self.config.count)]

    def _build_payment_method(
        self,
        index: int,
        accounts: list[AccountRecord],
        banks: list[BankRecord],
    ) -> PaymentMethodRecord:
        account = accounts[index % len(accounts)]
        bank = banks[(index * 3) % len(banks)]
        cluster_id = self.cluster_ids.build((index // max(1, self.config.cluster_size)) + 1)
        return PaymentMethodRecord(
            payment_method_id=self.payment_method_ids.build(index + 1),
            account_id=account.account_id,
            cluster_id=cluster_id,
            method_type=self.method_type_chooser.choose(self.rng),
            issuing_bank_id=bank.bank_id,
            currency=self.currency_chooser.choose(self.rng),
            created_at=format_timestamp(self.date_range.random_timestamp(self.rng)),
        )
