"""CLI entry point for the fraud dataset generator."""

from __future__ import annotations

import argparse
from pathlib import Path

from generator.config_loader import load_config
from generator.orchestrator.dataset_generator import DatasetGenerator


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Generate synthetic fraud datasets.")
    parser.add_argument("--base-dir", default=".", help="Project base directory containing config/ and output/.")
    parser.add_argument("--config", default=None, help="Path to default_config.json.")
    parser.add_argument("--scenarios", default=None, help="Path to scenario_config.json.")
    parser.add_argument("--output-dir", default=None, help="Directory where CSV files will be written.")
    parser.add_argument("--profile", choices=["small", "medium", "large"], default="large")
    parser.add_argument("--seed", type=int, default=None, help="Override random seed.")
    return parser


def main() -> int:
    args = build_parser().parse_args()
    base_dir = Path(args.base_dir).resolve()
    config_path = Path(args.config).resolve() if args.config else base_dir / "config" / "default_config.json"
    scenario_path = Path(args.scenarios).resolve() if args.scenarios else base_dir / "config" / "scenario_config.json"
    output_dir = Path(args.output_dir).resolve() if args.output_dir else base_dir / "output" / args.profile

    config = load_config(config_path, scenario_path, args.profile, args.seed)
    generator = DatasetGenerator(config, output_dir)
    stats = generator.generate()

    print(f"Generated profile={config.profile_name} seed={config.seed} output_dir={output_dir}")
    print(f"Accounts={stats.total_accounts} Events={stats.total_events} FraudAccounts={stats.fraud_accounts}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
