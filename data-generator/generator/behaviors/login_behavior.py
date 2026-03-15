"""Login-oriented normal behavior generation."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from random import Random

from generator.behaviors.graph_behavior import AccountGraphProfile, get_segment_behavior
from generator.entities.models import AccountRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp


@dataclass(frozen=True)
class LoginBehaviorResult:
    login_logs: list[dict[str, object]]
    login_sessions: list[dict[str, object]]


class LoginBehaviorGenerator:
    """Generates login logs and session rows for a normal segment."""

    def __init__(self, rng: Random, date_range: DateRange) -> None:
        self.rng = rng
        self.date_range = date_range
        self.login_ids = IdFactory("LGN", width=7, separator="")
        self.session_ids = IdFactory("SES", width=7, separator="")

    def generate(
        self,
        account: AccountRecord,
        graph_profile: AccountGraphProfile,
        segment_name: str,
    ) -> LoginBehaviorResult:
        behavior = get_segment_behavior(segment_name)
        login_count = self.rng.randint(*behavior.monthly_login_range)
        logs: list[dict[str, object]] = []
        sessions: list[dict[str, object]] = []
        for index in range(login_count):
            login_time = _natural_activity_time(self.rng, self.date_range, behavior.night_activity_weight, behavior.weekend_activity_weight)
            session_id = self.session_ids.build(index + 1)
            device_id = graph_profile.primary_device_ids[self.rng.randrange(len(graph_profile.primary_device_ids))]
            ip_id = graph_profile.primary_ip_ids[self.rng.randrange(len(graph_profile.primary_ip_ids))]
            logs.append(
                {
                    "login_log_id": self.login_ids.build(index + 1),
                    "account_id": account.account_id,
                    "session_id": session_id,
                    "device_id": device_id,
                    "ip_id": ip_id,
                    "login_time": format_timestamp(login_time),
                    "success": "true",
                    "auth_method": self.rng.choice(["password", "biometric", "password_otp"]),
                    "segment_name": segment_name,
                }
            )
            duration_minutes = self.rng.randint(*behavior.session_minutes_range)
            sessions.append(
                {
                    "session_id": session_id,
                    "account_id": account.account_id,
                    "device_id": device_id,
                    "ip_id": ip_id,
                    "started_at": format_timestamp(login_time),
                    "ended_at": format_timestamp(login_time + timedelta(minutes=duration_minutes)),
                    "page_count": max(1, int(self.rng.triangular(2, 18, 6))),
                    "segment_name": segment_name,
                }
            )
        return LoginBehaviorResult(login_logs=logs, login_sessions=sessions)


def _natural_activity_time(
    rng: Random,
    date_range: DateRange,
    night_weight: float,
    weekend_weight: float,
) -> datetime:
    for _ in range(64):
        candidate = date_range.random_timestamp(rng)
        hour = candidate.hour
        is_weekend = candidate.weekday() >= 5
        weight = 0.15
        if 7 <= hour <= 10:
            weight = 0.85
        elif 11 <= hour <= 17:
            weight = 1.0
        elif 18 <= hour <= 22:
            weight = 0.78
        elif 23 <= hour or hour <= 5:
            weight = max(0.02, night_weight)
        if is_weekend:
            weight *= 0.8 + weekend_weight
        if rng.random() <= min(1.0, weight):
            return candidate
    return date_range.random_timestamp(rng)
