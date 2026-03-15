"""Fraud scenario helpers."""

from generator.fraud.label_builder import build_labels
from generator.fraud.account_takeover import AccountTakeoverInjector
from generator.fraud.arbitrage import ArbitrageInjector
from generator.fraud.collusive_ring import CollusiveRingInjector
from generator.fraud.credential_stuffing import CredentialStuffingInjector
from generator.fraud.money_mule import MoneyMuleInjector
from generator.fraud.promotion_abuse import PromotionAbuseInjector
from generator.fraud.scenario_allocator import ScenarioAllocationConfig, ScenarioAllocator

__all__ = [
    "AccountTakeoverInjector",
    "ArbitrageInjector",
    "CollusiveRingInjector",
    "CredentialStuffingInjector",
    "MoneyMuleInjector",
    "PromotionAbuseInjector",
    "ScenarioAllocationConfig",
    "ScenarioAllocator",
    "build_labels",
]
