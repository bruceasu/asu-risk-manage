"""Money mule fraud scenario."""

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


class MoneyMuleInjector(FraudScenarioInjector):
    scenario_name = "money_mule"

    def select_targets(self, rng: Random, accounts, target_count: int, allocated_counts) -> list[ScenarioTarget]:
        ordered = sorted(accounts[:target_count], key=lambda account: (account.account_type, account.account_id))
        return [ScenarioTarget(account=account) for account in ordered]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        for index, target in enumerate(targets, start=1):
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips, prefer_risky_ip=True)
            payment_method = account_payment_methods(context.payment_methods, target.account.account_id)[0]
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            inbound_amount = round(180.0 * float(context.settings.get("avg_amount_multiplier", 2.1)), 2)
            result.rows["deposits"].append(
                {
                    "deposit_id": f"MULE_DEP_{index:07d}",
                    "account_id": target.account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "deposit_time": event_time,
                    "amount": f"{inbound_amount:.2f}",
                    "currency": target.account.currency,
                    "source_type": "p2p_inbound",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["transfers"].append(
                {
                    "transfer_id": f"MULE_XFR_{index:07d}",
                    "account_id": target.account.account_id,
                    "counterparty_cluster_id": target.account.cluster_id,
                    "transfer_time": event_time,
                    "amount": f"{inbound_amount * 0.85:.2f}",
                    "currency": target.account.currency,
                    "direction": "outbound",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["withdrawals"].append(
                {
                    "withdrawal_id": f"MULE_WDL_{index:07d}",
                    "account_id": target.account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "withdrawal_time": event_time,
                    "amount": f"{inbound_amount * 0.12:.2f}",
                    "currency": target.account.currency,
                    "channel": "atm",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_MULE_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "fan_in_credit",
                        "score": 0.9,
                    },
                    {
                        "event_id": f"SCN_MULE_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "layered_transfer",
                        "score": 0.93,
                    },
                ]
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"MULE_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.6),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="mule_exit_node",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(self.scenario_name, target.account.account_id, 0.93, "fan-in deposits followed by fast outbound transfers")
            )
        return result
