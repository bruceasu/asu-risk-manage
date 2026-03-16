
-- rule_definition
CREATE TABLE rule_definition (
    rule_code            VARCHAR(128) PRIMARY KEY,
    rule_name            VARCHAR(256) NOT NULL,
    category             VARCHAR(64) NOT NULL,
    description          TEXT NOT NULL,
    severity             VARCHAR(32) NOT NULL,
    owner_module         VARCHAR(64) NOT NULL,
    current_version      INT NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL
);

CREATE INDEX idx_rule_definition_category
ON rule_definition(category);
CREATE INDEX idx_rule_definition_active
ON rule_definition(is_active);

--    rule_codeRAPID_WITHDRAW_AFTER_DEPOSIT
--    category LOGIN, TRANSACTION, DEVICE, SECURITY, GRAPH, COMPOSITE
--    severity LOW, MEDIUM, HIGH, CRITICAL
--    current_version

-- rule_version
CREATE TABLE rule_version (
    rule_code            VARCHAR(128) NOT NULL,
    version              INT NOT NULL,
    parameter_json       TEXT NOT NULL,
    score_weight         INT NOT NULL,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from       TIMESTAMP NOT NULL,
    effective_to         TIMESTAMP,
    created_at           TIMESTAMP NOT NULL,
    created_by           VARCHAR(128),
    change_note          TEXT,
    PRIMARY KEY(rule_code, version)
);


CREATE INDEX idx_rule_version_effective
ON rule_version(rule_code, enabled, effective_from, effective_to);
-- parameter_json 键值示例：
-- {
--   "maxDelayMinutes": 30,
--   "minDepositCount24h": 1,
--   "minWithdrawCount24h": 1
-- }

--    effective_from / effective_to
--    change_note 
-- rule_hit_log
CREATE TABLE rule_hit_log (
    hit_id               BIGSERIAL PRIMARY KEY,
    account_id           VARCHAR(64) NOT NULL,
    rule_code            VARCHAR(128) NOT NULL,
    rule_version         INT NOT NULL,
    hit_time             TIMESTAMP NOT NULL,
    score                INT NOT NULL,
    reason_code          VARCHAR(128) NOT NULL,
    evidence_json        TEXT,
    feature_version      INT NOT NULL,
    evaluation_mode      VARCHAR(32) NOT NULL
);


CREATE INDEX idx_rule_hit_log_account_time
ON rule_hit_log(account_id, hit_time DESC);
CREATE INDEX idx_rule_hit_log_rule_time
ON rule_hit_log(rule_code, hit_time DESC);
-- evaluation_mode 可取值：
    -- BATCH
    -- REALTIME
-- evidence_json
-- {
--   "deposit_count_24h": 2,
--   "withdraw_count_24h": 2,
--   "withdraw_after_deposit_delay_avg_24h": 18.0,
--   "threshold_max_delay_minutes": 30
-- }

-- rule_evaluation_job
CREATE TABLE rule_evaluation_job (
    job_id                  BIGSERIAL PRIMARY KEY,
    job_type                VARCHAR(32) NOT NULL,
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(32) NOT NULL,
    target_account_count    INT,
    processed_account_count INT,
    hit_account_count       INT,
    failed_account_count    INT,
    error_message           TEXT
);
-- status
--     RUNNING
--     SUCCESS
--     FAILED
--     PARTIAL_SUCCESS

-- rule_config_reload_log
CREATE TABLE rule_config_reload_log (
    reload_id             BIGSERIAL PRIMARY KEY,
    reload_time           TIMESTAMP NOT NULL,
    status                VARCHAR(32) NOT NULL,
    loaded_rule_count     INT,
    error_message         TEXT
);


    
-- rule_definition
INSERT INTO rule_definition
(rule_code, rule_name, category, description, severity, owner_module, current_version, is_active, created_at, updated_at)
VALUES
('HIGH_RISK_IP_LOGIN', 'High Risk IP Login', 'LOGIN', 'Login activity from high risk IP addresses', 'HIGH', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('LOGIN_FAILURE_BURST', 'Login Failure Burst', 'LOGIN', 'Burst of failed login attempts within 24 hours', 'MEDIUM', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('RAPID_WITHDRAW_AFTER_DEPOSIT', 'Rapid Withdraw After Deposit', 'TRANSACTION', 'Withdrawal shortly after deposit', 'HIGH', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('SHARED_DEVICE_CLUSTER', 'Shared Device Cluster', 'DEVICE', 'Account belongs to suspicious shared device cluster', 'HIGH', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('RAPID_PROFILE_CHANGE', 'Rapid Profile Change', 'SECURITY', 'Rapid sequence of security/profile changes', 'MEDIUM', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('SECURITY_CHANGE_BEFORE_WITHDRAW', 'Security Change Before Withdraw', 'SECURITY', 'Security event shortly before withdrawal', 'HIGH', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('HIGH_RISK_NEIGHBOR_CLUSTER', 'High Risk Neighbor Cluster', 'GRAPH', 'High number of risky graph neighbors', 'HIGH', 'fraud-rule-engine', 1, TRUE, NOW(), NOW()),
('ATO_SUSPICION_COMPOSITE', 'ATO Suspicion Composite', 'COMPOSITE', 'Composite account takeover suspicion pattern', 'CRITICAL', 'fraud-rule-engine', 1, TRUE, NOW(), NOW());

-- rule_version
INSERT INTO rule_version
(rule_code, version, parameter_json, score_weight, enabled, effective_from, effective_to, created_at, created_by, change_note)
VALUES
('HIGH_RISK_IP_LOGIN', 1, '{"minHighRiskIpLoginCount24h":1}', 20, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('LOGIN_FAILURE_BURST', 1, '{"minLoginFailureCount24h":20,"minLoginFailureRate24h":0.8}', 12, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('RAPID_WITHDRAW_AFTER_DEPOSIT', 1, '{"maxDelayMinutes":30,"minDepositCount24h":1,"minWithdrawCount24h":1}', 25, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('SHARED_DEVICE_CLUSTER', 1, '{"minSharedDeviceAccounts7d":5}', 22, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('RAPID_PROFILE_CHANGE', 1, '{"requireRapidProfileChangeFlag":true}', 10, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('SECURITY_CHANGE_BEFORE_WITHDRAW', 1, '{"requireSecurityChangeBeforeWithdrawFlag":true}', 28, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('HIGH_RISK_NEIGHBOR_CLUSTER', 1, '{"minRiskNeighborCount30d":3,"minGraphClusterSize30d":5}', 24, TRUE, NOW(), NULL, NOW(), 'system', 'initial version'),
('ATO_SUSPICION_COMPOSITE', 1, '{"minNewDeviceLoginCount7d":1,"minHighRiskIpLoginCount24h":1,"requireSecurityChangeBeforeWithdrawFlag":true}', 40, TRUE, NOW(), NULL, NOW(), 'system', 'initial version');


