"""Deterministic bank entity generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import BankRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class BankGeneratorConfig:
    count: int
    country_weights: dict[str, float]


class BankGenerator:
    """Generates deterministic synthetic banks."""

    def __init__(self, rng: Random, date_range: DateRange, config: BankGeneratorConfig) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.bank_ids = IdFactory.bank_ids()
        self.country_chooser = WeightedChooser(config.country_weights)

    def generate(self) -> list[BankRecord]:
        return [self._build_bank(index) for index in range(self.config.count)]

    def _build_bank(self, index: int) -> BankRecord:
        country = self.country_chooser.choose(self.rng)
        suffix = f"{country}{index + 1:04d}"
        return BankRecord(
            bank_id=self.bank_ids.build(index + 1),
            bank_name=f"{country} Trust Bank {index + 1}",
            country=country,
            routing_code=f"RT{index + 1:07d}",
            swift_code=f"{suffix[:4]:<4}".replace(" ", "X") + country + "01",
            created_at=format_timestamp(self.date_range.random_timestamp(self.rng)),
        )
