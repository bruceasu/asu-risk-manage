"""Promotion abuse fraud scenario."""

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


class PromotionAbuseInjector(FraudScenarioInjector):
    scenario_name = "promotion_abuse"

    def select_targets(
        self,
        rng: Random,
        accounts,
        target_count: int,
        allocated_counts,
    ) -> list[ScenarioTarget]:
        return [ScenarioTarget(account=account) for account in accounts[:target_count]]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        for index, target in enumerate(targets, start=1):
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips)
            payment_method = account_payment_methods(context.payment_methods, target.account.account_id)[0]
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            reward_amount = round(25.0 * float(context.settings.get("avg_amount_multiplier", 1.4)), 2)
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_PROMO_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "promo_signup",
                        "score": 0.84,
                    },
                    {
                        "event_id": f"SCN_PROMO_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "promo_redeem",
                        "score": 0.91,
                    },
                    {
                        "event_id": f"SCN_PROMO_{index:07d}_3",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "instant_withdrawal",
                        "score": 0.95,
                    },
                ]
            )
            result.rows["deposits"].append(
                {
                    "deposit_id": f"FRD_DEP_{index:07d}",
                    "account_id": target.account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "deposit_time": event_time,
                    "amount": f"{reward_amount:.2f}",
                    "currency": target.account.currency,
                    "source_type": "promo_credit",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["withdrawals"].append(
                {
                    "withdrawal_id": f"FRD_WDL_{index:07d}",
                    "account_id": target.account.account_id,
                    "payment_method_id": payment_method.payment_method_id,
                    "withdrawal_time": event_time,
                    "amount": f"{reward_amount * 0.98:.2f}",
                    "currency": target.account.currency,
                    "channel": "bank_transfer",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"PROMO_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.45),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="shared_promo_artifact",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(self.scenario_name, target.account.account_id, 0.9, "promotion claim followed by immediate cash-out")
            )
        return result
