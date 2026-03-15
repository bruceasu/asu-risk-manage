"""Deterministic device entity generation."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import DeviceRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class DeviceGeneratorConfig:
    count: int
    device_type_weights: dict[str, float]
    os_weights: dict[str, float]
    platform_weights: dict[str, float]


class DeviceGenerator:
    """Generates deterministic synthetic devices."""

    def __init__(self, rng: Random, date_range: DateRange, config: DeviceGeneratorConfig) -> None:
        self.rng = rng
        self.date_range = date_range
        self.config = config
        self.device_ids = IdFactory.device_ids()
        self.device_type_chooser = WeightedChooser(config.device_type_weights)
        self.os_chooser = WeightedChooser(config.os_weights)
        self.platform_chooser = WeightedChooser(config.platform_weights)

    def generate(self) -> list[DeviceRecord]:
        return [self._build_device(index) for index in range(self.config.count)]

    def _build_device(self, index: int) -> DeviceRecord:
        device_type = self.device_type_chooser.choose(self.rng)
        operating_system = self.os_chooser.choose(self.rng)
        trust_floor = 25 if device_type == "mobile" else 40
        return DeviceRecord(
            device_id=self.device_ids.build(index + 1),
            platform=self.platform_chooser.choose(self.rng),
            trust_score=self.rng.randint(trust_floor, 99),
            device_type=device_type,
            operating_system=operating_system,
            created_at=format_timestamp(self.date_range.random_timestamp(self.rng)),
        )
