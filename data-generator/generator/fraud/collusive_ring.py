"""Collusive ring fraud scenario."""

from __future__ import annotations

from random import Random

from generator.fraud.fraud_base import (
    FraudInjectionResult,
    FraudScenarioInjector,
    ScenarioContext,
    ScenarioGraphLink,
    ScenarioTarget,
    choose_device_and_ip,
)
from generator.fraud.label_builder import build_labels
from generator.time_utils import format_timestamp


class CollusiveRingInjector(FraudScenarioInjector):
    scenario_name = "collusive_ring"

    def select_targets(self, rng: Random, accounts, target_count: int, allocated_counts) -> list[ScenarioTarget]:
        ordered = sorted(accounts[:target_count], key=lambda account: account.cluster_id)
        return [ScenarioTarget(account=account, overlap_index=index) for index, account in enumerate(ordered)]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        if not targets:
            return result
        for index, target in enumerate(targets, start=1):
            next_target = targets[index % len(targets)]
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips)
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            amount = round(95.0 * float(context.settings.get("avg_amount_multiplier", 1.6)), 2)
            result.rows["transfers"].append(
                {
                    "transfer_id": f"RING_XFR_{index:07d}",
                    "account_id": target.account.account_id,
                    "counterparty_cluster_id": next_target.account.cluster_id,
                    "transfer_time": event_time,
                    "amount": f"{amount:.2f}",
                    "currency": target.account.currency,
                    "direction": "outbound",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_RING_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "shared_beneficiary_ring",
                        "score": 0.84,
                    },
                    {
                        "event_id": f"SCN_RING_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "circular_funds_flow",
                        "score": 0.89,
                    },
                ]
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"RING_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.4),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="ring_member_shared_artifact",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(self.scenario_name, target.account.account_id, 0.86, "clustered accounts moving funds in a circular ring", target.overlap_index)
            )
        return result
