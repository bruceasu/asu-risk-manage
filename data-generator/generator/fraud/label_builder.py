"""Fraud label helpers."""

from __future__ import annotations

from generator.fraud.fraud_base import ScenarioLabel


def build_labels(
    scenario_name: str,
    account_id: str,
    confidence: float,
    reason: str,
    overlap_rank: int = 0,
) -> list[ScenarioLabel]:
    return [
        ScenarioLabel(
            account_id=account_id,
            scenario_name=scenario_name,
            is_fraud=True,
            confidence=max(0.0, min(1.0, confidence)),
            reason=reason,
            overlap_rank=overlap_rank,
        )
    ]
