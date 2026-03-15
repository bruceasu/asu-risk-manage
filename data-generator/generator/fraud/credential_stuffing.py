"""Credential stuffing fraud scenario."""

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


class CredentialStuffingInjector(FraudScenarioInjector):
    scenario_name = "credential_stuffing"

    def select_targets(self, rng: Random, accounts, target_count: int, allocated_counts) -> list[ScenarioTarget]:
        ordered = sorted(accounts[:target_count], key=lambda account: (account.kyc_level, account.account_id))
        return [ScenarioTarget(account=account, overlap_index=index) for index, account in enumerate(ordered)]

    def inject(self, context: ScenarioContext, targets: list[ScenarioTarget]) -> FraudInjectionResult:
        result = FraudInjectionResult.empty()
        for index, target in enumerate(targets, start=1):
            device, ip = choose_device_and_ip(context.rng, context.devices, context.ips, prefer_risky_ip=True)
            event_time = format_timestamp(context.date_range.random_timestamp(context.rng))
            for attempt in range(1, 5):
                result.rows["login_logs"].append(
                    {
                        "login_log_id": f"FRD_LGN_{index:07d}_{attempt}",
                        "account_id": target.account.account_id,
                        "session_id": f"FRD_SES_{index:07d}",
                        "device_id": device.device_id,
                        "ip_id": ip.ip_id,
                        "login_time": event_time,
                        "success": "true" if attempt == 4 else "false",
                        "auth_method": "password",
                        "segment_name": self.scenario_name,
                    }
                )
            result.rows["security_events"].append(
                {
                    "security_event_id": f"FRD_SEC_{index:07d}",
                    "account_id": target.account.account_id,
                    "device_id": device.device_id,
                    "ip_id": ip.ip_id,
                    "event_time": event_time,
                    "event_type": "credential_stuffing_detected",
                    "event_result": "challenge",
                    "segment_name": self.scenario_name,
                }
            )
            result.rows["scenario_events"].extend(
                [
                    {
                        "event_id": f"SCN_CRED_{index:07d}_1",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "password_spray",
                        "score": 0.91,
                    },
                    {
                        "event_id": f"SCN_CRED_{index:07d}_2",
                        "account_id": target.account.account_id,
                        "scenario_name": self.scenario_name,
                        "event_time": event_time,
                        "signal": "compromised_login",
                        "score": 0.88,
                    },
                ]
            )
            result.rows["fraud_scenarios"].append(
                {
                    "scenario_id": f"CRED_{index:07d}",
                    "account_id": target.account.account_id,
                    "scenario_name": self.scenario_name,
                    "intensity": context.settings.get("intensity", 0.5),
                    "created_at": event_time,
                }
            )
            result.graph_links.append(
                ScenarioGraphLink(
                    account_id=target.account.account_id,
                    device_id=device.device_id,
                    ip_id=ip.ip_id,
                    link_type="credential_attack_source",
                    scenario_name=self.scenario_name,
                )
            )
            result.labels.extend(
                build_labels(
                    self.scenario_name,
                    target.account.account_id,
                    0.87,
                    "multiple failed logins followed by a successful compromise",
                    target.overlap_index,
                )
            )
        return result
