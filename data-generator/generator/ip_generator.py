"""Deterministic IP entity generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import IpRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class IpGeneratorConfig:
    count: int
    country_weights: dict[str, float]
    ip_risk_weights: dict[str, float]


class IpGenerator:
    """Generates deterministic synthetic IP records."""

    def __init__(self, rng: Random, date_range: DateRange, config: IpGeneratorConfig) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.ip_ids = IdFactory("IP", width=7, separator="")
        self.country_chooser = WeightedChooser(config.country_weights)
        self.risk_chooser = WeightedChooser(config.ip_risk_weights)

    def generate(self) -> list[IpRecord]:
        return [self._build_ip(index) for index in range(self.config.count)]

    def _build_ip(self, index: int) -> IpRecord:
        risk_level = self.risk_chooser.choose(self.rng)
        risk_bands = {
            "low": (1, 35),
            "medium": (36, 70),
            "high": (71, 100),
        }
        low, high = risk_bands.get(risk_level, (1, 100))
        return IpRecord(
            ip_id=self.ip_ids.build(index + 1),
            ip_address=f"172.{index % 250}.{(index // 250) % 250}.{(index % 200) + 1}",
            country=self.country_chooser.choose(self.rng),
            risk_score=self.rng.randint(low, high),
            risk_level=risk_level,
            created_at=format_timestamp(self.date_range.random_timestamp(self.rng)),
        )
