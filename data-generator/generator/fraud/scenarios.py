"""Fraud scenario selection and scoring."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class FraudScenario:
    name: str
    intensity: float
    avg_event_boost: int
    avg_amount_multiplier: float


class FraudScenarioCatalog:
    """Provides deterministic fraud scenario assignments."""

    def __init__(self, weights: dict[str, float], settings: dict[str, dict[str, float]]) -> None:
        self.chooser = WeightedChooser(weights)
        self.settings = settings

    def pick(self, rng: Random) -> FraudScenario:
        name = self.chooser.choose(rng)
        config = self.settings.get(name, {})
        return FraudScenario(
            name=name,
            intensity=float(config.get("intensity", 0.3)),
            avg_event_boost=int(config.get("avg_event_boost", 2)),
            avg_amount_multiplier=float(config.get("avg_amount_multiplier", 1.25)),
        )
