"""Account takeover fraud scenario."""

from __future__ import annotations

from random import Random

from generator.fraud.fraud_base import (
    FraudInjectionResult,
    FraudScenarioInjector,
    ScenarioContext,
    ScenarioGraphLink,
    ScenarioTarget,
    account_payment_methods,
    choose_device_and_ip,
)
from generator.fraud.label_builder import build_labels
from generator.time_utils import format_timestamp


class AccountTakeoverInjector(FraudScenarioInjector):
    scenario_name = "account_takeover"

    def select_targets(self, rng: Random, accounts, target_count: int, allocated_counts) -> list[ScenarioTarget]:
        ordered = sorted(accounts[:target_count], key=lambda account: (account.risk_band, account.account_id), reverse=True)
        return [ScenarioTarget(account=account, overlap_index=index) for index, account in enumerate(ordered)]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        for index, target in enumerate(targets, start=1):
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips, prefer_risky_ip=True)
            payment_method = account_payment_methods(context.payment_methods, target.account.account_id)[0]
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            drain_amount = round(220.0 * float(context.settings.get("avg_amount_multiplier", 1.7)), 2)
            result.rows["security_events"].append(
                {
                    "security_event_id": f"ATO_SEC_{index:07d}",
                    "account_id": target.account.account_id,
                    "device_id": device.device_id,
                    "ip_id": ip.ip_id,
                    "event_time": event_time,
                    "event_type": "beneficiary_change",
                    "event_result": "success",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["transfers"].append(
                {
                    "transfer_id": f"ATO_XFR_{index:07d}",
                    "account_id": target.account.account_id,
                    "counterparty_cluster_id": target.account.cluster_id,
                    "transfer_time": event_time,
                    "amount": f"{drain_amount:.2f}",
                    "currency": target.account.currency,
                    "direction": "outbound",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["withdrawals"].append(
                {
                    "withdrawal_id": f"ATO_WDL_{index:07d}",
                    "account_id": target.account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "withdrawal_time": event_time,
                    "amount": f"{drain_amount * 0.92:.2f}",
                    "currency": target.account.currency,
                    "channel": "crypto_offramp",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_ATO_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "new_device_login",
                        "score": 0.9,
                    },
                    {
                        "event_id": f"SCN_ATO_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "beneficiary_added",
                        "score": 0.94,
                    },
                    {
                        "event_id": f"SCN_ATO_{index:07d}_3",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "rapid_funds_exit",
                        "score": 0.97,
                    },
                ]
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"ATO_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.55),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="takeover_endpoint",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(
                    self.scenario_name,
                    target.account.account_id,
                    0.95,
                    "compromise followed by beneficiary change and funds extraction",
                    target.overlap_index,
                )
            )
        return result
