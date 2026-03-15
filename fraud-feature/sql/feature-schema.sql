--     feature_definition
--     account_feature_snapshot
--     account_feature_history
--     feature_generation_job
--     feature_quality_check

-- 1 feature_definition
CREATE TABLE feature_definition (
    feature_name            VARCHAR(128) PRIMARY KEY,
    entity_type             VARCHAR(32) NOT NULL,
    data_type               VARCHAR(32) NOT NULL,
    category                VARCHAR(64) NOT NULL,
    description             TEXT NOT NULL,
    window_type             VARCHAR(32),
    window_value            INT,
    computation_mode        VARCHAR(32) NOT NULL,
    source_tables           TEXT NOT NULL,
    owner_module            VARCHAR(64) NOT NULL,
    feature_version         INT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    is_online_serving       BOOLEAN NOT NULL DEFAULT FALSE,
    is_ml_feature           BOOLEAN NOT NULL DEFAULT FALSE,
    is_rule_feature         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL
);
CREATE INDEX idx_feature_definition_category ON feature_definition(category);
CREATE INDEX idx_feature_definition_active ON feature_definition(is_active);

-- 2 account_feature_snapshot
CREATE TABLE account_feature_snapshot (
    account_id                              VARCHAR(64) PRIMARY KEY,
    feature_version                         INT NOT NULL,
    generated_at                            TIMESTAMP NOT NULL,
    account_age_days                        INT,
    kyc_level_numeric                       INT,
    registration_ip_risk_score              DOUBLE PRECISION,
    login_count_24h                         INT,
    login_failure_count_24h                 INT,
    login_failure_rate_24h                  DOUBLE PRECISION,
    unique_ip_count_24h                     INT,
    high_risk_ip_login_count_24h            INT,
    vpn_ip_login_count_24h                  INT,
    new_device_login_count_7d               INT,
    night_login_ratio_7d                    DOUBLE PRECISION,
    transaction_count_24h                   INT,
    total_amount_24h                        DOUBLE PRECISION,
    avg_transaction_amount_24h              DOUBLE PRECISION,
    deposit_count_24h                       INT,
    withdraw_count_24h                      INT,
    deposit_amount_24h                      DOUBLE PRECISION,
    withdraw_amount_24h                     DOUBLE PRECISION,
    deposit_withdraw_ratio_24h              DOUBLE PRECISION,
    unique_counterparty_count_24h           INT,
    withdraw_after_deposit_delay_avg_24h    DOUBLE PRECISION,
    rapid_withdraw_after_deposit_flag_24h   BOOLEAN,
    reward_transaction_count_30d            INT,
    reward_withdraw_delay_avg_30d           DOUBLE PRECISION,
    unique_device_count_7d                  INT,
    device_switch_count_24h                 INT,
    shared_device_accounts_7d               INT,
    security_event_count_24h                INT,
    rapid_profile_change_flag_24h           BOOLEAN,
    security_change_before_withdraw_flag_24h BOOLEAN,
    shared_ip_accounts_7d                   INT,
    shared_bank_accounts_30d                INT,
    graph_cluster_size_30d                  INT,
    risk_neighbor_count_30d                 INT,
    anomaly_score_last                      DOUBLE PRECISION
);

CREATE INDEX idx_feature_snapshot_generated ON account_feature_snapshot(generated_at);

-- 3 account_feature_history
CREATE TABLE account_feature_history (
    snapshot_id                             BIGSERIAL PRIMARY KEY,
    snapshot_time                           TIMESTAMP NOT NULL,
    account_id                              VARCHAR(64) NOT NULL,
    feature_version                         INT NOT NULL,
    account_age_days                        INT,
    kyc_level_numeric                       INT,
    registration_ip_risk_score              DOUBLE PRECISION,
    login_count_24h                         INT,
    login_failure_count_24h                 INT,
    login_failure_rate_24h                  DOUBLE PRECISION,
    unique_ip_count_24h                     INT,
    high_risk_ip_login_count_24h            INT,
    vpn_ip_login_count_24h                  INT,
    new_device_login_count_7d               INT,
    night_login_ratio_7d                    DOUBLE PRECISION,
    transaction_count_24h                   INT,
    total_amount_24h                        DOUBLE PRECISION,
    avg_transaction_amount_24h              DOUBLE PRECISION,
    deposit_count_24h                       INT,
    withdraw_count_24h                      INT,
    deposit_amount_24h                      DOUBLE PRECISION,
    withdraw_amount_24h                     DOUBLE PRECISION,
    deposit_withdraw_ratio_24h              DOUBLE PRECISION,
    unique_counterparty_count_24h           INT,
    withdraw_after_deposit_delay_avg_24h    DOUBLE PRECISION,
    rapid_withdraw_after_deposit_flag_24h   BOOLEAN,
    reward_transaction_count_30d            INT,
    reward_withdraw_delay_avg_30d           DOUBLE PRECISION,
    unique_device_count_7d                  INT,
    device_switch_count_24h                 INT,
    shared_device_accounts_7d               INT,
    security_event_count_24h                INT,
    rapid_profile_change_flag_24h           BOOLEAN,
    security_change_before_withdraw_flag_24h BOOLEAN,
    shared_ip_accounts_7d                   INT,
    shared_bank_accounts_30d                INT,
    graph_cluster_size_30d                  INT,
    risk_neighbor_count_30d                 INT,
    anomaly_score_last                      DOUBLE PRECISION
);
CREATE INDEX idx_feature_history_account_time ON account_feature_history(account_id, snapshot_time DESC);

-- 4 feature_generation_job
CREATE TABLE feature_generation_job (
    job_id                  BIGSERIAL PRIMARY KEY,
    job_type                VARCHAR(32) NOT NULL,
    feature_version         INT NOT NULL,
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(32) NOT NULL,
    target_account_count    INT,
    processed_account_count INT,
    failed_account_count    INT,
    error_message           TEXT
);

-- 5 feature_quality_check
CREATE TABLE feature_quality_check (
    check_id                BIGSERIAL PRIMARY KEY,
    check_time              TIMESTAMP NOT NULL,
    feature_version         INT NOT NULL,
    feature_name            VARCHAR(128) NOT NULL,
    check_type              VARCHAR(64) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    total_records           INT,
    failed_records          INT,
    details                 TEXT
);



-- ------------------------------------------
CREATE TABLE accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    account_type VARCHAR(16),
    country VARCHAR(8),
    kyc_level INT,
    created_at TIMESTAMP,
    registration_ip VARCHAR(64),
    status VARCHAR(16)
);
CREATE TABLE account_profiles (
    account_id VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(32),
    birth_year INT,
    risk_score INT DEFAULT 0
);
CREATE TABLE account_balance (
    account_id VARCHAR(64),
    currency VARCHAR(8),
    balance NUMERIC(18,2),
    updated_at TIMESTAMP
);
CREATE TABLE account_limits (
    account_id VARCHAR(64),
    daily_withdraw_limit NUMERIC,
    daily_transfer_limit NUMERIC
);
CREATE TABLE devices (
    device_id VARCHAR(64) PRIMARY KEY,
    device_type VARCHAR(32),
    os VARCHAR(32),
    created_at TIMESTAMP
);
CREATE TABLE device_fingerprint (
    device_id VARCHAR(64),
    browser VARCHAR(32),
    os_version VARCHAR(32),
    screen_resolution VARCHAR(32),
    timezone VARCHAR(32)
);
CREATE TABLE account_devices (
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    first_seen TIMESTAMP,
    last_seen TIMESTAMP
);
CREATE TABLE device_clusters (
    cluster_id VARCHAR(64),
    device_id VARCHAR(64)
);

CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    ip VARCHAR(64),
    success BOOLEAN,
    login_time TIMESTAMP
);

CREATE TABLE login_failures (
    id BIGSERIAL,
    account_id VARCHAR(64),
    ip VARCHAR(64),
    attempt_time TIMESTAMP
);
CREATE TABLE login_sessions (
    session_id VARCHAR(64),
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    login_time TIMESTAMP,
    logout_time TIMESTAMP
);
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(64),
    counterparty_account VARCHAR(64),
    amount NUMERIC(18,2),
    currency VARCHAR(8),
    transaction_type VARCHAR(16),
    created_at TIMESTAMP
);
CREATE TABLE deposits (
    deposit_id BIGSERIAL,
    account_id VARCHAR(64),
    amount NUMERIC,
    source VARCHAR(32),
    created_at TIMESTAMP
);
CREATE TABLE withdrawals (
    withdraw_id BIGSERIAL,
    account_id VARCHAR(64),
    amount NUMERIC,
    target VARCHAR(64),
    created_at TIMESTAMP
);
CREATE TABLE transfers (
    transfer_id BIGSERIAL,
    from_account VARCHAR(64),
    to_account VARCHAR(64),
    amount NUMERIC,
    created_at TIMESTAMP
);
CREATE TABLE payment_methods (
    payment_id VARCHAR(64),
    account_id VARCHAR(64),
    method_type VARCHAR(32),
    created_at TIMESTAMP
);
CREATE TABLE ip_addresses (
    ip VARCHAR(64) PRIMARY KEY,
    country VARCHAR(8),
    isp VARCHAR(64)
);
CREATE TABLE ip_intelligence (
    ip VARCHAR(64),
    risk_level VARCHAR(16),
    is_vpn BOOLEAN,
    is_proxy BOOLEAN,
    is_datacenter BOOLEAN
);
CREATE TABLE security_events (
    event_id BIGSERIAL,
    account_id VARCHAR(64),
    event_type VARCHAR(32),
    created_at TIMESTAMP
);

-- password_change
-- email_change
-- 2fa_disable
CREATE TABLE password_resets (
    reset_id BIGSERIAL,
    account_id VARCHAR(64),
    reset_time TIMESTAMP
);
CREATE TABLE account_graph_edges (
    from_account VARCHAR(64),
    to_account VARCHAR(64),
    edge_type VARCHAR(32),
    created_at TIMESTAMP
);

-- transaction
-- shared_device
-- shared_ip
-- shared_bank
CREATE TABLE account_clusters (
    cluster_id VARCHAR(64),
    account_id VARCHAR(64)
);
-- fraud rings
CREATE TABLE bank_accounts (
    bank_account_id VARCHAR(64),
    account_id VARCHAR(64),
);
CREATE TABLE fraud_labels (
    account_id VARCHAR(64),
    fraud_type VARCHAR(32),
    labeled_at TIMESTAMP
);


--   - bonus_abuse
--   - credential_stuffing
--   - account_takeover
--   - money_mule
--   - collusion_ring

CREATE TABLE investigation_cases (
    case_id BIGSERIAL,
    account_id VARCHAR(64),
    risk_score DOUBLE PRECISION,
    created_at TIMESTAMP
);
