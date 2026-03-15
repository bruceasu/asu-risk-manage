\echo Importing fraud-data-generator CSV files with psql \copy

\copy accounts from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/accounts.csv' with (format csv, header true)
\copy account_profiles from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/account_profiles.csv' with (format csv, header true)
\copy account_balances from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/account_balances.csv' with (format csv, header true)
\copy devices from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/devices.csv' with (format csv, header true)
\copy ips from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/ips.csv' with (format csv, header true)
\copy merchants from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/merchants.csv' with (format csv, header true)
\copy beneficiaries from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/beneficiaries.csv' with (format csv, header true)

\copy account_device_links from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/account_device_links.csv' with (format csv, header true)
\copy account_ip_links from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/account_ip_links.csv' with (format csv, header true)
\copy device_ip_links from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/device_ip_links.csv' with (format csv, header true)

\copy login_events from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/login_events.csv' with (format csv, header true)
\copy session_events from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/session_events.csv' with (format csv, header true)
\copy page_views from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/page_views.csv' with (format csv, header true)
\copy payment_attempts from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/payment_attempts.csv' with (format csv, header true)
\copy card_transactions from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/card_transactions.csv' with (format csv, header true)
\copy bank_transfers from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/bank_transfers.csv' with (format csv, header true)
\copy cash_out_events from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/cash_out_events.csv' with (format csv, header true)
\copy p2p_transfers from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/p2p_transfers.csv' with (format csv, header true)
\copy chargebacks from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/chargebacks.csv' with (format csv, header true)

\copy alerts from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/alerts.csv' with (format csv, header true)
\copy cases from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/cases.csv' with (format csv, header true)

\copy fraud_labels from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/fraud_labels.csv' with (format csv, header true)
\copy fraud_scenarios from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/fraud_scenarios.csv' with (format csv, header true)
\copy scenario_events from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/scenario_events.csv' with (format csv, header true)
\copy generation_metrics from 'D:/03_projects/suk/asu-trading-analysis/data-generator/output/schema-smoke/generation_metrics.csv' with (format csv, header true)

\echo Import complete
