"""Fraud scenario allocation logic."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import AccountRecord
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class ScenarioAllocationConfig:
    fraud_ratio: float
    scenario_distribution: dict[str, float]
    overlap_pairs: dict[str, tuple[str, ...]]


class ScenarioAllocator:
    """Allocates accounts to fraud scenarios deterministically."""

    def __init__(self, rng: Random, config: ScenarioAllocationConfig) -> None:
        self.rng = rng
        self.config = config
        self.chooser = WeightedChooser(config.scenario_distribution)

    def allocate(self, accounts: list[AccountRecord]) -> dict[str, list[AccountRecord]]:
        fraud_target = max(1, int(len(accounts) * self.config.fraud_ratio))
        allocation: dict[str, list[AccountRecord]] = {name: [] for name in self.config.scenario_distribution}
        fraud_accounts = accounts[:fraud_target]
        for account in fraud_accounts:
            scenario_name = self.chooser.choose(self.rng)
            allocation[scenario_name].append(account)
        self._allocate_overlaps(allocation)
        return allocation

    def _allocate_overlaps(self, allocation: dict[str, list[AccountRecord]]) -> None:
        for source, overlap_targets in self.config.overlap_pairs.items():
            source_accounts = allocation.get(source, [])
            overlap_count = max(0, len(source_accounts) // 3)
            for index in range(overlap_count):
                account = source_accounts[index]
                for target in overlap_targets:
                    allocation.setdefault(target, []).append(account)
