"""Dataset orchestration."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from random import Random

from generator.behaviors.profiles import profile_for_risk
from generator.config_loader import GeneratorConfig
from generator.csv_writer import ChunkedCsvWriter
from generator.entities.models import AccountRecord, DeviceRecord, IpRecord
from generator.fraud.scenarios import FraudScenarioCatalog
from generator.id_factory import IdFactory
from generator.schema_constants import CSV_SCHEMAS
from generator.time_utils import format_timestamp, parse_iso8601, random_timestamp
from generator.weighted_random import WeightedChooser


@dataclass(frozen=True)
class GenerationStats:
    total_accounts: int
    total_events: int
    fraud_accounts: int


class DatasetGenerator:
    """Coordinates entity and event generation."""

    def __init__(self, config: GeneratorConfig, output_dir: Path) -> None:
        self.config = config
        self.output_dir = output_dir
        self.rng = Random(config.seed)
        self.start_time = parse_iso8601(config.start_time)
        self.end_time = parse_iso8601(config.end_time)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self.status_chooser = WeightedChooser(config.account_status_weights)
        self.country_chooser = WeightedChooser(config.country_weights)
        self.risk_chooser = WeightedChooser(config.risk_band_weights)
        self.scenario_catalog = FraudScenarioCatalog(config.fraud_distribution, config.scenario_settings)

        self.account_ids = IdFactory("acct")
        self.customer_ids = IdFactory("cust")
        self.device_ids = IdFactory("dev")
        self.ip_ids = IdFactory("ip")
        self.merchant_ids = IdFactory("merch")
        self.beneficiary_ids = IdFactory("bene")
        self.event_ids = IdFactory("evt")
        self.alert_ids = IdFactory("alrt")
        self.case_ids = IdFactory("case")
        self.scenario_ids = IdFactory("scn")
        self.session_ids = IdFactory("sess")

    def generate(self) -> GenerationStats:
        writers = {
            name: ChunkedCsvWriter(self.output_dir / name, fields, self.config.chunk_size)
            for name, fields in CSV_SCHEMAS.items()
        }
        try:
            return self._generate_all(writers)
        finally:
            for writer in writers.values():
                writer.close()

    def _generate_all(self, writers: dict[str, ChunkedCsvWriter]) -> GenerationStats:
        profile = self.config.profile
        account_count = int(profile["account_count"])
        device_count = int(profile["device_count"])
        ip_count = int(profile["ip_count"])
        merchant_count = int(profile["merchant_count"])
        beneficiary_count = int(profile["beneficiary_count"])
        total_events = int(profile["total_events"])
        fraud_accounts_target = max(1, int(account_count * self.config.fraud_ratio))

        accounts = self._write_accounts(writers, account_count, fraud_accounts_target)
        devices = self._write_devices(writers, device_count)
        ips = self._write_ips(writers, ip_count)
        merchants = self._write_merchants(writers, merchant_count)
        beneficiaries = self._write_beneficiaries(writers, beneficiary_count)
        event_counts = self._event_counts(total_events)

        self._write_links(writers, accounts, devices, ips)
        self._write_events(writers, accounts, devices, ips, merchants, beneficiaries, event_counts)
        self._write_metrics(writers, account_count, total_events, fraud_accounts_target, event_counts)
        return GenerationStats(account_count, total_events, fraud_accounts_target)

    def _write_accounts(
        self,
        writers: dict[str, ChunkedCsvWriter],
        account_count: int,
        fraud_accounts_target: int,
    ) -> list[AccountRecord]:
        accounts: list[AccountRecord] = []
        for index in range(account_count):
            account_id = self.account_ids.build(index + 1)
            customer_id = self.customer_ids.build(index + 1)
            country = self.country_chooser.choose(self.rng)
            currency = self.config.currency_by_country[country]
            status = self.status_chooser.choose(self.rng)
            risk_band = self.risk_chooser.choose(self.rng)
            scenario_name = ""
            if index < fraud_accounts_target:
                scenario_name = self.scenario_catalog.pick(self.rng).name

            created_at = format_timestamp(random_timestamp(self.rng, self.start_time, self.end_time))
            base_balance = round(self.rng.uniform(50.0, 25000.0), 2)
            account = AccountRecord(
                account_id=account_id,
                customer_id=customer_id,
                country=country,
                currency=currency,
                status=status,
                risk_band=risk_band,
                fraud_segment=scenario_name or "clean",
                base_balance=base_balance,
            )
            accounts.append(account)

            writers["accounts.csv"].write_row(
                {
                    "account_id": account_id,
                    "customer_id": customer_id,
                    "country": country,
                    "currency": currency,
                    "status": status,
                    "risk_band": risk_band,
                    "created_at": created_at,
                }
            )
            writers["account_profiles.csv"].write_row(
                {
                    "account_id": account_id,
                    "segment": "vip" if self.rng.random() < 0.06 else "retail",
                    "kyc_level": self.rng.choice(["standard", "enhanced", "simplified"]),
                    "email_domain": self.rng.choice(["mail.com", "example.org", "secure.net", "bank.test"]),
                    "phone_country_code": f"+{self.rng.choice([1, 44, 49, 65, 81, 61])}",
                }
            )
            writers["account_balances.csv"].write_row(
                {
                    "account_id": account_id,
                    "available_balance": f"{base_balance:.2f}",
                    "ledger_balance": f"{base_balance + self.rng.uniform(-20, 50):.2f}",
                    "balance_updated_at": created_at,
                }
            )
            writers["fraud_labels.csv"].write_row(
                {
                    "account_id": account_id,
                    "is_fraud": "true" if scenario_name else "false",
                    "scenario_name": scenario_name,
                    "label_reason": scenario_name or "baseline_behavior",
                    "labeled_at": created_at,
                }
            )
            if scenario_name:
                intensity = self.config.scenario_settings[scenario_name]["intensity"]
                writers["fraud_scenarios.csv"].write_row(
                    {
                        "scenario_id": self.scenario_ids.build(index + 1),
                        "account_id": account_id,
                        "scenario_name": scenario_name,
                        "intensity": intensity,
                        "created_at": created_at,
                    }
                )
        return accounts

    def _write_devices(self, writers: dict[str, ChunkedCsvWriter], count: int) -> list[DeviceRecord]:
        devices: list[DeviceRecord] = []
        for index in range(count):
            device = DeviceRecord(
                device_id=self.device_ids.build(index + 1),
                platform=self.rng.choice(["ios", "android", "web", "desktop"]),
                trust_score=self.rng.randint(20, 99),
            )
            devices.append(device)
            writers["devices.csv"].write_row(
                {
                    "device_id": device.device_id,
                    "platform": device.platform,
                    "trust_score": device.trust_score,
                    "first_seen_at": format_timestamp(random_timestamp(self.rng, self.start_time, self.end_time)),
                }
            )
        return devices

    def _write_ips(self, writers: dict[str, ChunkedCsvWriter], count: int) -> list[IpRecord]:
        ips: list[IpRecord] = []
        for index in range(count):
            country = self.country_chooser.choose(self.rng)
            ip = IpRecord(
                ip_id=self.ip_ids.build(index + 1),
                ip_address=f"10.{index % 250}.{(index // 250) % 250}.{(index % 200) + 1}",
                country=country,
                risk_score=self.rng.randint(1, 100),
            )
            ips.append(ip)
            writers["ips.csv"].write_row(
                {
                    "ip_id": ip.ip_id,
                    "ip_address": ip.ip_address,
                    "country": ip.country,
                    "risk_score": ip.risk_score,
                    "first_seen_at": format_timestamp(random_timestamp(self.rng, self.start_time, self.end_time)),
                }
            )
        return ips

    def _write_merchants(self, writers: dict[str, ChunkedCsvWriter], count: int) -> list[str]:
        merchants: list[str] = []
        categories = ["travel", "gaming", "grocery", "electronics", "digital_goods", "luxury"]
        for index in range(count):
            merchant_id = self.merchant_ids.build(index + 1)
            merchants.append(merchant_id)
            writers["merchants.csv"].write_row(
                {
                    "merchant_id": merchant_id,
                    "merchant_name": f"Merchant {index + 1}",
                    "merchant_category": categories[index % len(categories)],
                    "country": self.country_chooser.choose(self.rng),
                }
            )
        return merchants

    def _write_beneficiaries(self, writers: dict[str, ChunkedCsvWriter], count: int) -> list[str]:
        beneficiaries: list[str] = []
        for index in range(count):
            beneficiary_id = self.beneficiary_ids.build(index + 1)
            beneficiaries.append(beneficiary_id)
            writers["beneficiaries.csv"].write_row(
                {
                    "beneficiary_id": beneficiary_id,
                    "beneficiary_name": f"Beneficiary {index + 1}",
                    "country": self.country_chooser.choose(self.rng),
                    "bank_code": f"B{1000 + (index % 9000)}",
                }
            )
        return beneficiaries

    def _write_links(
        self,
        writers: dict[str, ChunkedCsvWriter],
        accounts: list[AccountRecord],
        devices: list[DeviceRecord],
        ips: list[IpRecord],
    ) -> None:
        for index, account in enumerate(accounts):
            device = devices[index % len(devices)]
            ip = ips[index % len(ips)]
            linked_at = format_timestamp(random_timestamp(self.rng, self.start_time, self.end_time))
            writers["account_device_links.csv"].write_row(
                {
                    "account_id": account.account_id,
                    "device_id": device.device_id,
                    "linked_at": linked_at,
                    "is_primary": "true",
                }
            )
            writers["account_ip_links.csv"].write_row(
                {
                    "account_id": account.account_id,
                    "ip_id": ip.ip_id,
                    "linked_at": linked_at,
                    "usage_count": self.rng.randint(1, 250),
                }
            )
            writers["device_ip_links.csv"].write_row(
                {
                    "device_id": device.device_id,
                    "ip_id": ip.ip_id,
                    "linked_at": linked_at,
                    "confidence": round(self.rng.uniform(0.5, 0.99), 3),
                }
            )

    def _event_counts(self, total_events: int) -> dict[str, int]:
        counts = {
            event_type: int(total_events * weight)
            for event_type, weight in self.config.event_type_weights.items()
        }
        current_total = sum(counts.values())
        event_names = list(self.config.event_type_weights)
        index = 0
        while current_total < total_events:
            counts[event_names[index % len(event_names)]] += 1
            current_total += 1
            index += 1
        while current_total > total_events:
            name = event_names[index % len(event_names)]
            if counts[name] > 0:
                counts[name] -= 1
                current_total -= 1
            index += 1
        return counts

    def _write_events(
        self,
        writers: dict[str, ChunkedCsvWriter],
        accounts: list[AccountRecord],
        devices: list[DeviceRecord],
        ips: list[IpRecord],
        merchants: list[str],
        beneficiaries: list[str],
        event_counts: dict[str, int],
    ) -> None:
        global_event_number = 0
        alert_counter = 0
        case_counter = 0
        for event_type, count in event_counts.items():
            for _ in range(count):
                global_event_number += 1
                account = accounts[self.rng.randrange(len(accounts))]
                profile = profile_for_risk(account.risk_band)
                device = devices[self.rng.randrange(len(devices))]
                ip = ips[self.rng.randrange(len(ips))]
                event_id = self.event_ids.build(global_event_number)
                timestamp = format_timestamp(random_timestamp(self.rng, self.start_time, self.end_time))
                session_id = self.session_ids.build(self.rng.randint(1, max(2, len(accounts))))
                amount = round(self.rng.uniform(5.0, profile.average_amount * 4), 2)
                merchant_id = merchants[self.rng.randrange(len(merchants))]
                beneficiary_id = beneficiaries[self.rng.randrange(len(beneficiaries))]

                if event_type == "login_events":
                    writers["login_events.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "device_id": device.device_id,
                            "ip_id": ip.ip_id,
                            "event_time": timestamp,
                            "success": "true" if self.rng.random() > 0.08 else "false",
                            "risk_score": ip.risk_score,
                        }
                    )
                elif event_type == "session_events":
                    writers["session_events.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "session_id": session_id,
                            "device_id": device.device_id,
                            "event_time": timestamp,
                            "duration_seconds": self.rng.randint(30, 3600),
                        }
                    )
                elif event_type == "page_views":
                    writers["page_views.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "session_id": session_id,
                            "page_name": self.rng.choice(["home", "wallet", "checkout", "offers", "security"]),
                            "event_time": timestamp,
                            "dwell_ms": self.rng.randint(50, 9000),
                        }
                    )
                elif event_type == "payment_attempts":
                    writers["payment_attempts.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "merchant_id": merchant_id,
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                            "decision": self.rng.choice(["approve", "challenge", "decline"]),
                        }
                    )
                elif event_type == "card_transactions":
                    writers["card_transactions.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "merchant_id": merchant_id,
                            "card_last4": f"{self.rng.randint(0, 9999):04d}",
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                        }
                    )
                elif event_type == "bank_transfers":
                    writers["bank_transfers.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "beneficiary_id": beneficiary_id,
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                            "direction": self.rng.choice(["outbound", "inbound"]),
                        }
                    )
                elif event_type == "cash_out_events":
                    writers["cash_out_events.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                            "channel": self.rng.choice(["atm", "branch", "crypto_offramp"]),
                        }
                    )
                elif event_type == "p2p_transfers":
                    counterparty = accounts[self.rng.randrange(len(accounts))]
                    writers["p2p_transfers.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "counterparty_account_id": counterparty.account_id,
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                        }
                    )
                elif event_type == "chargebacks":
                    writers["chargebacks.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "original_event_id": self.event_ids.build(self.rng.randint(1, global_event_number)),
                            "amount": f"{amount:.2f}",
                            "currency": account.currency,
                            "event_time": timestamp,
                            "reason_code": self.rng.choice(["fraud", "dispute", "processing_error"]),
                        }
                    )
                elif event_type == "alerts":
                    alert_counter += 1
                    writers["alerts.csv"].write_row(
                        {
                            "alert_id": self.alert_ids.build(alert_counter),
                            "account_id": account.account_id,
                            "rule_name": self.rng.choice(["velocity_spike", "geo_anomaly", "device_mismatch", "high_risk_payee"]),
                            "severity": self.rng.choice(["low", "medium", "high"]),
                            "event_time": timestamp,
                            "status": self.rng.choice(["open", "triaged", "closed"]),
                        }
                    )
                elif event_type == "cases":
                    case_counter += 1
                    writers["cases.csv"].write_row(
                        {
                            "case_id": self.case_ids.build(case_counter),
                            "account_id": account.account_id,
                            "alert_id": self.alert_ids.build(max(1, alert_counter)),
                            "case_type": self.rng.choice(["fraud_review", "chargeback_review", "kyc_refresh"]),
                            "opened_at": timestamp,
                            "status": self.rng.choice(["new", "investigating", "closed"]),
                        }
                    )
                elif event_type == "scenario_events":
                    scenario_name = account.fraud_segment if account.fraud_segment != "clean" else self.scenario_catalog.pick(self.rng).name
                    writers["scenario_events.csv"].write_row(
                        {
                            "event_id": event_id,
                            "account_id": account.account_id,
                            "scenario_name": scenario_name,
                            "event_time": timestamp,
                            "signal": self.rng.choice(["burst_login", "proxy_chain", "promo_farm", "mule_fanout"]),
                            "score": round(self.rng.uniform(0.45, 0.99), 3),
                        }
                    )

    def _write_metrics(
        self,
        writers: dict[str, ChunkedCsvWriter],
        account_count: int,
        total_events: int,
        fraud_accounts_target: int,
        event_counts: dict[str, int],
    ) -> None:
        metrics = {
            "profile": self.config.profile_name,
            "seed": self.config.seed,
            "account_count": account_count,
            "total_events": total_events,
            "fraud_accounts": fraud_accounts_target,
            "output_file_count": len(CSV_SCHEMAS),
        }
        metrics.update({f"events_{name}": count for name, count in sorted(event_counts.items())})
        for name, value in metrics.items():
            writers["generation_metrics.csv"].write_row({"metric_name": name, "metric_value": value})
