"""Arbitrage fraud scenario."""

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


class ArbitrageInjector(FraudScenarioInjector):
    scenario_name = "arbitrage"

    def select_targets(self, rng: Random, accounts, target_count: int, allocated_counts) -> list[ScenarioTarget]:
        ordered = sorted(accounts[:target_count], key=lambda account: (account.currency, account.account_id))
        return [ScenarioTarget(account=account) for account in ordered]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        for index, target in enumerate(targets, start=1):
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips)
            payment_method = account_payment_methods(context.payment_methods, target.account.account_id)[0]
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            amount = round(75.0 * float(context.settings.get("avg_amount_multiplier", 1.3)), 2)
            result.rows["transactions"].extend(
                [
                    {
                        "transaction_id": f"ARB_TRX_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "payment_method_id": payment_method.payment_method_id,
                        "device_id": device.device_id,
                        "ip_id": ip.ip_id,
                        "transaction_time": event_time,
                        "amount": f"{amount:.2f}",
                        "currency": target.account.currency,
                        "merchant_category": "gift_cards",
                        "segment_name": self.scenario_name,
                    },
                    {
                        "transaction_id": f"ARB_TRX_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "payment_method_id": payment_method.payment_method_id,
                        "device_id": device.device_id,
                        "ip_id": ip.ip_id,
                        "transaction_time": event_time,
                        "amount": f"{amount * 1.08:.2f}",
                        "currency": target.account.currency,
                        "merchant_category": "resale_market",
                        "segment_name": self.scenario_name,
                    },
                ]
            )
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_ARB_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "price_dislocation_capture",
                        "score": 0.79,
                    },
                    {
                        "event_id": f"SCN_ARB_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "rapid_resale",
                        "score": 0.82,
                    },
                ]
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"ARB_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.35),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="arbitrage_terminal",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(self.scenario_name, target.account.account_id, 0.78, "paired buy-resell cycle exploiting pricing gap")
            )
        return result
