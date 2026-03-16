一、rule_engine_schema.sql
这是 Rule Engine 的最小生产级 schema，支持：
    • 规则定义
    • 规则版本
    • 规则命中日志
    • 规则执行任务
    • 规则配置热加载

1. rule_definition
规则基础定义表。
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
建议索引：
CREATE INDEX idx_rule_definition_category
ON rule_definition(category);
CREATE INDEX idx_rule_definition_active
ON rule_definition(is_active);
字段说明：
    • rule_code：唯一编码，如 RAPID_WITHDRAW_AFTER_DEPOSIT
    • category：LOGIN, TRANSACTION, DEVICE, SECURITY, GRAPH, COMPOSITE
    • severity：LOW, MEDIUM, HIGH, CRITICAL
    • current_version：当前生效版本号

2. rule_version
规则版本与参数表。
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
建议索引：
CREATE INDEX idx_rule_version_effective
ON rule_version(rule_code, enabled, effective_from, effective_to);
parameter_json 示例：
{
  "maxDelayMinutes": 30,
  "minDepositCount24h": 1,
  "minWithdrawCount24h": 1
}
说明：
    • 规则逻辑在 Java 中
    • 阈值、分数、启用状态在 DB 中
    • effective_from / effective_to 支持时间生效窗口
    • change_note 便于审计

3. rule_hit_log
规则命中审计表。
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
建议索引：
CREATE INDEX idx_rule_hit_log_account_time
ON rule_hit_log(account_id, hit_time DESC);
CREATE INDEX idx_rule_hit_log_rule_time
ON rule_hit_log(rule_code, hit_time DESC);
evaluation_mode 建议值：
    • BATCH
    • REALTIME
evidence_json 示例：
{
  "deposit_count_24h": 2,
  "withdraw_count_24h": 2,
  "withdraw_after_deposit_delay_avg_24h": 18.0,
  "threshold_max_delay_minutes": 30
}

4. rule_evaluation_job
批量规则执行任务表。
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
状态建议：
    • RUNNING
    • SUCCESS
    • FAILED
    • PARTIAL_SUCCESS

5. rule_config_reload_log
配置热加载日志。
CREATE TABLE rule_config_reload_log (
    reload_id             BIGSERIAL PRIMARY KEY,
    reload_time           TIMESTAMP NOT NULL,
    status                VARCHAR(32) NOT NULL,
    loaded_rule_count     INT,
    error_message         TEXT
);
用途：
    • 记录 JVM 缓存刷新
    • 排查“为什么线上还没生效”

二、推荐初始规则数据
建议在初始化脚本中先插入一批核心规则。

1. rule_definition 初始化示例
INSERT INTO rule_definition
(rule_code, rule_name, category, description, severity, owner_module, current_version, is_active, created_at, updated_at)
VALUES
('HIGH_RISK_IP_LOGIN', 'High Risk IP Login', 'LOGIN', 'Login activity from high risk IP addresses', 'HIGH', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('LOGIN_FAILURE_BURST', 'Login Failure Burst', 'LOGIN', 'Burst of failed login attempts within 24 hours', 'MEDIUM', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('RAPID_WITHDRAW_AFTER_DEPOSIT', 'Rapid Withdraw After Deposit', 'TRANSACTION', 'Withdrawal shortly after deposit', 'HIGH', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('SHARED_DEVICE_CLUSTER', 'Shared Device Cluster', 'DEVICE', 'Account belongs to suspicious shared device cluster', 'HIGH', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('RAPID_PROFILE_CHANGE', 'Rapid Profile Change', 'SECURITY', 'Rapid sequence of security/profile changes', 'MEDIUM', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('SECURITY_CHANGE_BEFORE_WITHDRAW', 'Security Change Before Withdraw', 'SECURITY', 'Security event shortly before withdrawal', 'HIGH', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('HIGH_RISK_NEIGHBOR_CLUSTER', 'High Risk Neighbor Cluster', 'GRAPH', 'High number of risky graph neighbors', 'HIGH', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW()),
('ATO_SUSPICION_COMPOSITE', 'ATO Suspicion Composite', 'COMPOSITE', 'Composite account takeover suspicion pattern', 'CRITICAL', 'fraud-rule-engine-library', 1, TRUE, NOW(), NOW());

2. rule_version 初始化示例
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

三、fraud-rule-engine 模块 AI 代码生成 Prompt
下面是一套可直接用于生成代码的 Prompt，建议按顺序逐个使用，而不是一次性全生成。
[fraud-rule-engine.prompt.md](../prompts/fraud-rule-engine.prompt.md)

四、推荐第一阶段上线规则
第一阶段建议先做这 8 条，够用且价值高：
    1. HIGH_RISK_IP_LOGIN
    2. LOGIN_FAILURE_BURST
    3. RAPID_WITHDRAW_AFTER_DEPOSIT
    4. REWARD_WITHDRAW_ABUSE
    5. SHARED_DEVICE_CLUSTER
    6. RAPID_PROFILE_CHANGE
    7. SECURITY_CHANGE_BEFORE_WITHDRAW
    8. ATO_SUSPICION_COMPOSITE
这组规则已经能覆盖：
    • 撞库
    • 盗号
    • 羊毛党
    • 团伙共享设备
    • 提现风险

五、与其他模块的依赖关系
建议依赖方向如下：
fraud-feature
    ↓
fraud-rule-engine-core
    ↓
fraud-rule-engine-library
    ↓
fraud-risk
    ↓
fraud-case
关键点：
    • fraud-rule-engine 读取 Feature Store
    • 不直接依赖 Python ML
    • fraud-risk 可以把规则分和 ML 分融合
    • fraud-case 和 fraud-ai 直接消费规则结果

六、落地建议
你当前最合理的推进顺序是：
    1. feature_store_schema.sql
    2. fraud-feature
    3. rule_engine_schema.sql
    4. fraud-rule-engine-core
    5. fraud-rule-engine-library
    6. fraud-risk
这样一旦 Rule Engine 落地，你就已经有一套可解释、可审计、可上线的反欺诈核心。
下一步最自然的是继续补上：
fraud-risk 的生产级设计 + schema + AI 代码生成 Prompt。
