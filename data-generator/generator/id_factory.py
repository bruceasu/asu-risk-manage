"""Deterministic identifier generation."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class IdFormat:
    """Defines a stable identifier shape."""

    prefix: str
    width: int
    separator: str = ""


class IdFactory:
    """Builds stable prefixed identifiers."""

    def __init__(self, prefix: str, width: int = 9, separator: str = "_") -> None:
        self.format = IdFormat(prefix=prefix, width=width, separator=separator)

    def build(self, value: int) -> str:
        if value < 0:
            raise ValueError("Identifier value must be non-negative.")
        return format_id(self.format, value)

    @classmethod
    def account_ids(cls) -> "IdFactory":
        return cls("ACC", width=7, separator="")

    @classmethod
    def device_ids(cls) -> "IdFactory":
        return cls("DEV", width=7, separator="")

    @classmethod
    def bank_ids(cls) -> "IdFactory":
        return cls("BANK", width=7, separator="")

    @classmethod
    def cluster_ids(cls) -> "IdFactory":
        return cls("CLS", width=6, separator="")


def format_id(id_format: IdFormat, value: int) -> str:
    """Formats a numeric value using the provided identifier shape."""

    return f"{id_format.prefix}{id_format.separator}{value:0{id_format.width}d}"
