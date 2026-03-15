"""Graph and reuse behavior for normal account segments."""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from generator.entities.models import AccountRecord, DeviceRecord, IpRecord


@dataclass(frozen=True)
class SegmentBehavior:
    name: str
    monthly_login_range: tuple[int, int]
    monthly_transaction_range: tuple[int, int]
    device_pool_range: tuple[int, int]
    ip_pool_range: tuple[int, int]
    session_minutes_range: tuple[int, int]
    security_event_rate: float
    deposit_ratio: float
    withdrawal_ratio: float
    transfer_ratio: float
    night_activity_weight: float
    weekend_activity_weight: float


SEGMENT_BEHAVIORS: dict[str, SegmentBehavior] = {
    "low_activity_normal": SegmentBehavior(
        name="low_activity_normal",
        monthly_login_range=(5, 18),
        monthly_transaction_range=(2, 8),
        device_pool_range=(1, 2),
        ip_pool_range=(1, 3),
        session_minutes_range=(3, 18),
        security_event_rate=0.03,
        deposit_ratio=0.28,
        withdrawal_ratio=0.12,
        transfer_ratio=0.18,
        night_activity_weight=0.08,
        weekend_activity_weight=0.18,
    ),
    "medium_activity_normal": SegmentBehavior(
        name="medium_activity_normal",
        monthly_login_range=(18, 45),
        monthly_transaction_range=(8, 20),
        device_pool_range=(1, 3),
        ip_pool_range=(2, 5),
        session_minutes_range=(5, 24),
        security_event_rate=0.05,
        deposit_ratio=0.22,
        withdrawal_ratio=0.15,
        transfer_ratio=0.2,
        night_activity_weight=0.12,
        weekend_activity_weight=0.22,
    ),
    "high_activity_normal": SegmentBehavior(
        name="high_activity_normal",
        monthly_login_range=(40, 90),
        monthly_transaction_range=(18, 50),
        device_pool_range=(2, 4),
        ip_pool_range=(3, 8),
        session_minutes_range=(6, 30),
        security_event_rate=0.07,
        deposit_ratio=0.18,
        withdrawal_ratio=0.16,
        transfer_ratio=0.26,
        night_activity_weight=0.2,
        weekend_activity_weight=0.28,
    ),
    "dormant_accounts": SegmentBehavior(
        name="dormant_accounts",
        monthly_login_range=(0, 3),
        monthly_transaction_range=(0, 2),
        device_pool_range=(1, 1),
        ip_pool_range=(1, 2),
        session_minutes_range=(1, 8),
        security_event_rate=0.01,
        deposit_ratio=0.45,
        withdrawal_ratio=0.05,
        transfer_ratio=0.05,
        night_activity_weight=0.04,
        weekend_activity_weight=0.12,
    ),
    "new_users": SegmentBehavior(
        name="new_users",
        monthly_login_range=(8, 28),
        monthly_transaction_range=(1, 10),
        device_pool_range=(1, 2),
        ip_pool_range=(2, 4),
        session_minutes_range=(4, 20),
        security_event_rate=0.09,
        deposit_ratio=0.32,
        withdrawal_ratio=0.08,
        transfer_ratio=0.1,
        night_activity_weight=0.14,
        weekend_activity_weight=0.25,
    ),
}


@dataclass(frozen=True)
class AccountGraphProfile:
    account_id: str
    segment_name: str
    primary_device_ids: tuple[str, ...]
    primary_ip_ids: tuple[str, ...]


def get_segment_behavior(segment_name: str) -> SegmentBehavior:
    if segment_name not in SEGMENT_BEHAVIORS:
        raise ValueError(f"Unknown normal segment: {segment_name}")
    return SEGMENT_BEHAVIORS[segment_name]


def build_account_graph_profile(
    rng: Random,
    account: AccountRecord,
    devices: list[DeviceRecord],
    ips: list[IpRecord],
    segment_name: str,
) -> AccountGraphProfile:
    if not devices:
        raise ValueError("devices must not be empty.")
    if not ips:
        raise ValueError("ips must not be empty.")
    behavior = get_segment_behavior(segment_name)
    device_count = rng.randint(*behavior.device_pool_range)
    ip_count = rng.randint(*behavior.ip_pool_range)
    start_offset = rng.randrange(len(devices))
    ip_offset = rng.randrange(len(ips))
    selected_devices = tuple(devices[(start_offset + index) % len(devices)].device_id for index in range(device_count))
    selected_ips = tuple(ips[(ip_offset + index) % len(ips)].ip_id for index in range(ip_count))
    return AccountGraphProfile(
        account_id=account.account_id,
        segment_name=segment_name,
        primary_device_ids=selected_devices,
        primary_ip_ids=selected_ips,
    )
