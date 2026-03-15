"""Security event generation for normal segments."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.behaviors.graph_behavior import AccountGraphProfile, get_segment_behavior
from generator.behaviors.login_behavior import _natural_activity_time
from generator.entities.models import AccountRecord
from generator.id_factory import IdFactory
from generator.time_utils import DateRange, format_timestamp


@dataclass(frozen=True)
class SecurityBehaviorResult:
    security_events: list[dict[str, object]]


class SecurityBehaviorGenerator:
    """Generates low-rate normal security events."""

    def __init__(self, rng: Random, date_range: DateRange) -> None:
        self.rng = rng
        self.date_range = date_range
        self.security_ids = IdFactory("SEC", width=7, separator="")

    def generate(
        self,
        account: AccountRecord,
        graph_profile: AccountGraphProfile,
        segment_name: str,
    ) -> SecurityBehaviorResult:
        behavior = get_segment_behavior(segment_name)
        events: list[dict[str, object]] = []
        event_count = _sample_security_count(self.rng, behavior.security_event_rate)
        for index in range(event_count):
            event_time = _natural_activity_time(
                self.rng,
                self.date_range,
                behavior.night_activity_weight,
                behavior.weekend_activity_weight,
            )
            event_type = _pick_security_event(self.rng, segment_name)
            events.append(
                {
                    "security_event_id": self.security_ids.build(index + 1),
                    "account_id": account.account_id,
                    "device_id": graph_profile.primary_device_ids[index % len(graph_profile.primary_device_ids)],
                    "ip_id": graph_profile.primary_ip_ids[index % len(graph_profile.primary_ip_ids)],
                    "event_time": format_timestamp(event_time),
                    "event_type": event_type,
                    "event_result": "success" if event_type in {"password_change", "2fa_enrolled"} else "info",
                    "segment_name": segment_name,
                }
            )
        return SecurityBehaviorResult(security_events=events)


def _sample_security_count(rng: Random, rate: float) -> int:
    count = 0
    for _ in range(8):
        if rng.random() < rate:
            count += 1
    return count


def _pick_security_event(rng: Random, segment_name: str) -> str:
    if segment_name == "new_users":
        return rng.choice(["2fa_enrolled", "password_reset", "email_verified", "new_device_verified"])
    if segment_name == "dormant_accounts":
        return rng.choice(["password_reset", "profile_review", "login_challenge"])
    if segment_name == "high_activity_normal":
        return rng.choice(["2fa_challenge", "trusted_device_added", "password_change", "profile_review"])
    return rng.choice(["login_challenge", "password_change", "trusted_device_added"])
