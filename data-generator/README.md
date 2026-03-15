# data-generator

`data-generator` creates deterministic synthetic CSV datasets for testing a fraud detection platform and loading the results into PostgreSQL quickly. The generator is standard-library only, writes files incrementally, and is designed to produce a mostly normal dataset with configurable fraud patterns layered in.

## Project Purpose

Use this project when you need:

- repeatable fraud and normal-behavior datasets for development or testing
- PostgreSQL-friendly CSV files with a stable column order
- multiple size profiles from smoke testing to large-volume generation
- explainable fraud scenarios instead of isolated random anomalies

The generator is optimized for practical test data generation rather than perfect real-world statistical fidelity.

## Project Layout

```text
data-generator/
|-- README.md
|-- config/
|   |-- default_config.json
|   `-- scenario_config.json
|-- generator/
|   |-- main.py
|   |-- config_loader.py
|   |-- schema_constants.py
|   |-- schema_validation.py
|   |-- csv_writer.py
|   |-- entities/
|   |-- behaviors/
|   |-- fraud/
|   `-- orchestrator/
`-- output/
```

## Generated Output Files

The generator currently writes 25 CSV files:

1. `accounts.csv`
2. `account_profiles.csv`
3. `account_balances.csv`
4. `devices.csv`
5. `ips.csv`
6. `merchants.csv`
7. `beneficiaries.csv`
8. `login_events.csv`
9. `session_events.csv`
10. `page_views.csv`
11. `payment_attempts.csv`
12. `card_transactions.csv`
13. `bank_transfers.csv`
14. `cash_out_events.csv`
15. `p2p_transfers.csv`
16. `chargebacks.csv`
17. `alerts.csv`
18. `cases.csv`
19. `account_device_links.csv`
20. `account_ip_links.csv`
21. `device_ip_links.csv`
22. `fraud_labels.csv`
23. `fraud_scenarios.csv`
24. `scenario_events.csv`
25. `generation_metrics.csv`

These files are emitted with headers and UTF-8 encoding and are ready for PostgreSQL `COPY ... CSV HEADER`.

## Configuration Files

### `config/default_config.json`

Base runtime configuration:

- random seed
- global date range
- size profiles
- event type weights
- country and currency distributions
- risk band and account status distributions
- overall fraud ratio

### `config/scenario_config.json`

Fraud configuration:

- scenario distribution
- per-scenario intensity
- average event boost
- average amount multiplier

Edit these two files first if you want to change scale, distributions, or fraud emphasis without touching Python code.

## How To Run The Generator

Run from the `data-generator` directory.

Generate the large profile into the default output folder:

```bash
python -m generator.main --base-dir . --profile large
```

Generate a small smoke dataset into a dedicated folder:

```bash
python -m generator.main --base-dir . --profile small --output-dir output/smoke
```

Generate a medium dataset with a custom seed:

```bash
python -m generator.main --base-dir . --profile medium --seed 99
```

Use explicit config paths:

```bash
python -m generator.main --base-dir . \
  --config config/default_config.json \
  --scenarios config/scenario_config.json \
  --profile small
```

The CLI prints the selected profile, seed, output folder, account count, total events, and fraud-account count.

## Dataset Profiles

Profiles live in `config/default_config.json`.

### `small`

Use this for smoke tests and schema validation.

- 1,000 accounts
- about 100,000 total events
- fast local runs
- good for validating PostgreSQL imports and downstream pipelines

### `medium`

Use this for realistic local or CI-scale integration testing.

- 10,000 accounts
- about 1,000,000 total events
- useful for end-to-end feature testing and performance checks

### `large`

Use this for high-volume staging or load-style testing.

- 100,000 accounts
- about 10,000,000 total events
- intended target scale for the project
- requires more disk space and longer runtime, but still writes incrementally

## Fraud Scenario Descriptions

The generator supports these explainable fraud scenarios:

### `promotion_abuse`

Event chain:
- account creation or promo enrollment
- reward claim
- very fast withdrawal or cash-out of the bonus value

### `credential_stuffing`

Event chain:
- repeated failed login attempts from concentrated infrastructure
- eventual successful login
- security signal or challenge event

### `account_takeover`

Event chain:
- suspicious login from a new device or IP
- beneficiary or payout-change behavior
- fast outbound transfer or withdrawal

### `money_mule`

Event chain:
- inbound credits or fan-in deposits
- short holding period
- layered outbound transfer and partial cash-out

### `arbitrage`

Event chain:
- buy at one venue or product path
- rapid resale or monetization path
- repeatable profit-seeking pattern around price or channel mismatch

### `collusive_ring`

Event chain:
- linked accounts share graph artifacts or relationship patterns
- repeated circular or ring-like transfers
- coordinated movement of funds inside the group

Some overlap is supported, especially between `credential_stuffing` and `account_takeover`.

## PostgreSQL COPY Import Examples

Replace `/absolute/path/to/output` with your real output directory.

### Core Entities

```sql
COPY accounts FROM '/absolute/path/to/output/accounts.csv' WITH (FORMAT csv, HEADER true);
COPY account_profiles FROM '/absolute/path/to/output/account_profiles.csv' WITH (FORMAT csv, HEADER true);
COPY account_balances FROM '/absolute/path/to/output/account_balances.csv' WITH (FORMAT csv, HEADER true);
COPY devices FROM '/absolute/path/to/output/devices.csv' WITH (FORMAT csv, HEADER true);
COPY ips FROM '/absolute/path/to/output/ips.csv' WITH (FORMAT csv, HEADER true);
COPY merchants FROM '/absolute/path/to/output/merchants.csv' WITH (FORMAT csv, HEADER true);
COPY beneficiaries FROM '/absolute/path/to/output/beneficiaries.csv' WITH (FORMAT csv, HEADER true);
```

### Login And Session Activity

```sql
COPY login_events FROM '/absolute/path/to/output/login_events.csv' WITH (FORMAT csv, HEADER true);
COPY session_events FROM '/absolute/path/to/output/session_events.csv' WITH (FORMAT csv, HEADER true);
COPY page_views FROM '/absolute/path/to/output/page_views.csv' WITH (FORMAT csv, HEADER true);
```

### Payment And Funds Movement

```sql
COPY payment_attempts FROM '/absolute/path/to/output/payment_attempts.csv' WITH (FORMAT csv, HEADER true);
COPY card_transactions FROM '/absolute/path/to/output/card_transactions.csv' WITH (FORMAT csv, HEADER true);
COPY bank_transfers FROM '/absolute/path/to/output/bank_transfers.csv' WITH (FORMAT csv, HEADER true);
COPY cash_out_events FROM '/absolute/path/to/output/cash_out_events.csv' WITH (FORMAT csv, HEADER true);
COPY p2p_transfers FROM '/absolute/path/to/output/p2p_transfers.csv' WITH (FORMAT csv, HEADER true);
COPY chargebacks FROM '/absolute/path/to/output/chargebacks.csv' WITH (FORMAT csv, HEADER true);
```

### Alerts, Cases, And Graph Links

```sql
COPY alerts FROM '/absolute/path/to/output/alerts.csv' WITH (FORMAT csv, HEADER true);
COPY cases FROM '/absolute/path/to/output/cases.csv' WITH (FORMAT csv, HEADER true);
COPY account_device_links FROM '/absolute/path/to/output/account_device_links.csv' WITH (FORMAT csv, HEADER true);
COPY account_ip_links FROM '/absolute/path/to/output/account_ip_links.csv' WITH (FORMAT csv, HEADER true);
COPY device_ip_links FROM '/absolute/path/to/output/device_ip_links.csv' WITH (FORMAT csv, HEADER true);
```

### Fraud-Specific Outputs

```sql
COPY fraud_labels FROM '/absolute/path/to/output/fraud_labels.csv' WITH (FORMAT csv, HEADER true);
COPY fraud_scenarios FROM '/absolute/path/to/output/fraud_scenarios.csv' WITH (FORMAT csv, HEADER true);
COPY scenario_events FROM '/absolute/path/to/output/scenario_events.csv' WITH (FORMAT csv, HEADER true);
COPY generation_metrics FROM '/absolute/path/to/output/generation_metrics.csv' WITH (FORMAT csv, HEADER true);
```

If you load into a fresh schema, import dimension-style files like `accounts`, `devices`, `ips`, `merchants`, and `beneficiaries` first, then load activity and fraud files.

## Deterministic Generation

Generation is deterministic when:

- the same `seed` is used
- the same profile is used
- the same config files are used
- the same code version is used

This is useful for:

- reproducible tests
- debugging downstream fraud rules
- comparing feature-engineering changes against a fixed input dataset

Override the seed on the command line with `--seed`, or change it in `config/default_config.json`.

## Scalability And Memory Usage

The generator is designed to keep memory usage practical by streaming CSV rows directly to disk instead of building full output tables in memory.

What this means in practice:

- rows are written incrementally through the chunked CSV writer
- schema validation is lightweight and runs per row
- the large profile targets high output volume without needing to hold all event rows in RAM
- compact entity catalogs still live in memory, so disk usage and runtime matter more than row-buffer memory

For large runs:

- write to a fast local disk
- make sure the output directory has enough free space
- start with `small` or `medium` if you are validating schema or import logic
- use a fixed seed when comparing results across runs

## Practical Quick Start

1. Edit `config/default_config.json` if you want a different seed or profile sizes.
2. Edit `config/scenario_config.json` if you want different fraud scenario weights.
3. Run `python -m generator.main --base-dir . --profile small --output-dir output/quickstart`.
4. Import the CSVs you care about with PostgreSQL `COPY`.
5. Check `generation_metrics.csv` to confirm the generated profile and event counts.
