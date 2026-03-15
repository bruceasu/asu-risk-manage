"""Time helper functions."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from random import Random


def parse_iso8601(value: str) -> datetime:
    return datetime.fromisoformat(value)


def format_timestamp(value: datetime) -> str:
    return value.isoformat(timespec="seconds")


@dataclass(frozen=True)
class DateRange:
    """Inclusive date range used by generators."""

    start: datetime
    end: datetime

    def __post_init__(self) -> None:
        if self.end < self.start:
            raise ValueError("end must be greater than or equal to start.")

    @classmethod
    def from_iso8601(cls, start: str, end: str) -> "DateRange":
        return cls(parse_iso8601(start), parse_iso8601(end))

    def random_timestamp(self, rng: Random) -> datetime:
        seconds = int((self.end - self.start).total_seconds())
        if seconds <= 0:
            return self.start
        return self.start + timedelta(seconds=rng.randint(0, seconds))


def random_timestamp(rng: Random, start: datetime, end: datetime) -> datetime:
    return DateRange(start, end).random_timestamp(rng)
