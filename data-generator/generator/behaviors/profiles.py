"""Behavior profiles for baseline activity."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class BehaviorProfile:
    login_velocity: int
    payment_velocity: int
    average_amount: float
    session_depth: int


def profile_for_risk(risk_band: str) -> BehaviorProfile:
    if risk_band == "high":
        return BehaviorProfile(login_velocity=8, payment_velocity=5, average_amount=180.0, session_depth=14)
    if risk_band == "medium":
        return BehaviorProfile(login_velocity=5, payment_velocity=3, average_amount=120.0, session_depth=10)
    return BehaviorProfile(login_velocity=3, payment_velocity=2, average_amount=75.0, session_depth=7)
