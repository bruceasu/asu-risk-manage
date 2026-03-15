"""Deterministic account entity generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import AccountRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class AccountGeneratorConfig:
    count: int
    country_weights: dict[str, float]
    kyc_level_weights: dict[str, float]
    account_type_weights: dict[str, float]
    status_weights: dict[str, float]
    risk_band_weights: dict[str, float]
    fraud_ratio: float
    currency_by_country: dict[str, str]
    cluster_size: int = 8


class AccountGenerator:
    """Generates deterministic synthetic accounts."""

    def __init__(self, rng: Random, date_range: DateRange, config: AccountGeneratorConfig) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.account_ids = IdFactory.account_ids()
        self.customer_ids = IdFactory("CUST", width=7, separator="")
        self.cluster_ids = IdFactory.cluster_ids()
        self.country_chooser = WeightedChooser(config.country_weights)
        self.kyc_chooser = WeightedChooser(config.kyc_level_weights)
        self.account_type_chooser = WeightedChooser(config.account_type_weights)
        self.status_chooser = WeightedChooser(config.status_weights)
        self.risk_chooser = WeightedChooser(config.risk_band_weights)

    def generate(self) -> list[AccountRecord]:
        return [self._build_account(index) for index in range(self.config.count)]

    def _build_account(self, index: int) -> AccountRecord:
        account_id = self.account_ids.build(index + 1)
        customer_id = self.customer_ids.build(index + 1)
        cluster_id = self.cluster_ids.build((index // max(1, self.config.cluster_size)) + 1)
        country = self.country_chooser.choose(self.rng)
        created_at = format_timestamp(self.date_range.random_timestamp(self.rng))
        fraud_segment = "fraud" if index < int(self.config.count * self.config.fraud_ratio) else "clean"
        currency = self.config.currency_by_country.get(country, "USD")
        base_balance = round(self.rng.uniform(25.0, 50000.0), 2)
        return AccountRecord(
            account_id=account_id,
            customer_id=customer_id,
            country=country,
            currency=currency,
            status=self.status_chooser.choose(self.rng),
            risk_band=self.risk_chooser.choose(self.rng),
            fraud_segment=fraud_segment,
            base_balance=base_balance,
            kyc_level=self.kyc_chooser.choose(self.rng),
            account_type=self.account_type_chooser.choose(self.rng),
            created_at=created_at,
            cluster_id=cluster_id,
        )
