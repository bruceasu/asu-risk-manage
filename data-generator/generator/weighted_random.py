"""Weighted random helpers."""

from __future__ import annotations

from bisect import bisect
from collections.abc import Iterable, Sequence
from random import Random
from typing import Generic, TypeVar

T = TypeVar("T")


def normalize_weights(weights: dict[T, float]) -> dict[T, float]:
    """Normalizes a non-empty mapping of non-negative weights."""

    if not weights:
        raise ValueError("Weights must not be empty.")
    total = 0.0
    normalized: dict[T, float] = {}
    for key, value in weights.items():
        numeric = float(value)
        if numeric < 0.0:
            raise ValueError(f"Weight for {key!r} must be non-negative.")
        normalized[key] = numeric
        total += numeric
    if total <= 0.0:
        raise ValueError("Weights must sum to a positive number.")
    return {key: value / total for key, value in normalized.items()}


class WeightedChooser(Generic[T]):
    """Draws values using deterministic weighted selection."""

    def __init__(self, weights: dict[T, float]) -> None:
        self.keys: list[T] = []
        self.breakpoints: list[float] = []
        total = 0.0
        for key, value in normalize_weights(weights).items():
            total += value
            self.keys.append(key)
            self.breakpoints.append(total)
        self.total = total

    def choose(self, rng: Random) -> T:
        target = rng.random() * self.total
        index = bisect(self.breakpoints, target)
        return self.keys[index]

    def sample(self, rng: Random, count: int) -> list[T]:
        return [self.choose(rng) for _ in range(count)]


def cycle_choices(values: Sequence[T], count: int) -> Iterable[T]:
    """Repeats a sequence deterministically until `count` values are yielded."""

    if count < 0:
        raise ValueError("count must be non-negative.")
    if not values and count:
        raise ValueError("values must not be empty when count is positive.")
    for index in range(count):
        yield values[index % len(values)]
