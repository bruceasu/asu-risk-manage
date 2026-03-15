"""Configuration loading helpers."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class GeneratorConfig:
    seed: int
    start_time: str
    end_time: str
    profile_name: str
    profile: dict[str, Any]
    event_type_weights: dict[str, float]
    account_status_weights: dict[str, float]
    country_weights: dict[str, float]
    currency_by_country: dict[str, str]
    risk_band_weights: dict[str, float]
    fraud_ratio: float
    fraud_distribution: dict[str, float]
    scenario_settings: dict[str, dict[str, float]]
    chunk_size: int


def _load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError(f"Config at {path} must be a JSON object.")
    return data


def _require_mapping(config: dict[str, Any], key: str) -> dict[str, Any]:
    value = config.get(key)
    if not isinstance(value, dict) or not value:
        raise ValueError(f"Missing or invalid mapping for '{key}'.")
    return value


def _normalize_weights(name: str, weights: dict[str, Any]) -> dict[str, float]:
    normalized: dict[str, float] = {}
    total = 0.0
    for key, value in weights.items():
        if not isinstance(key, str):
            raise ValueError(f"Weight key in '{name}' must be a string.")
        if not isinstance(value, (int, float)) or value < 0:
            raise ValueError(f"Weight '{name}.{key}' must be a non-negative number.")
        normalized[key] = float(value)
        total += float(value)
    if total <= 0:
        raise ValueError(f"Weight mapping '{name}' must sum to a positive value.")
    return {key: value / total for key, value in normalized.items()}


def load_config(
    config_path: Path,
    scenario_path: Path,
    profile_name: str,
    seed_override: int | None = None,
) -> GeneratorConfig:
    base_config = _load_json(config_path)
    scenario_config = _load_json(scenario_path)

    profiles = _require_mapping(base_config, "profiles")
    if profile_name not in profiles:
        known_profiles = ", ".join(sorted(profiles))
        raise ValueError(f"Unknown profile '{profile_name}'. Expected one of: {known_profiles}.")

    profile = profiles[profile_name]
    if not isinstance(profile, dict):
        raise ValueError(f"Profile '{profile_name}' must be an object.")

    seed_value = seed_override if seed_override is not None else base_config.get("seed", 42)
    if not isinstance(seed_value, int):
        raise ValueError("Seed must be an integer.")

    chunk_size = profile.get("chunk_size", 10000)
    if not isinstance(chunk_size, int) or chunk_size <= 0:
        raise ValueError("chunk_size must be a positive integer.")

    return GeneratorConfig(
        seed=seed_value,
        start_time=str(base_config["start_time"]),
        end_time=str(base_config["end_time"]),
        profile_name=profile_name,
        profile=profile,
        event_type_weights=_normalize_weights("event_type_weights", _require_mapping(base_config, "event_type_weights")),
        account_status_weights=_normalize_weights("account_status_weights", _require_mapping(base_config, "account_status_weights")),
        country_weights=_normalize_weights("country_weights", _require_mapping(base_config, "country_weights")),
        currency_by_country=_require_mapping(base_config, "currency_by_country"),
        risk_band_weights=_normalize_weights("risk_band_weights", _require_mapping(base_config, "risk_band_weights")),
        fraud_ratio=float(base_config.get("fraud_ratio", 0.01)),
        fraud_distribution=_normalize_weights("fraud_distribution", _require_mapping(scenario_config, "fraud_distribution")),
        scenario_settings=_require_mapping(scenario_config, "scenario_settings"),
        chunk_size=chunk_size,
    )
