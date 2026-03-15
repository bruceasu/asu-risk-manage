涓€銆乺ule_engine_schema.sql
杩欐槸 Rule Engine 鐨勬渶灏忕敓浜х骇 schema锛屾敮鎸侊細
    鈥?瑙勫垯瀹氫箟
    鈥?瑙勫垯鐗堟湰
    鈥?瑙勫垯鍛戒腑鏃ュ織
    鈥?瑙勫垯鎵ц浠诲姟
    鈥?瑙勫垯閰嶇疆鐑姞杞?

1. rule_definition
瑙勫垯鍩虹瀹氫箟琛ㄣ€?
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
寤鸿绱㈠紩锛?
CREATE INDEX idx_rule_definition_category
ON rule_definition(category);
CREATE INDEX idx_rule_definition_active
ON rule_definition(is_active);
瀛楁璇存槑锛?
    鈥?rule_code锛氬敮涓€缂栫爜锛屽 RAPID_WITHDRAW_AFTER_DEPOSIT
    鈥?category锛歀OGIN, TRANSACTION, DEVICE, SECURITY, GRAPH, COMPOSITE
    鈥?severity锛歀OW, MEDIUM, HIGH, CRITICAL
    鈥?current_version锛氬綋鍓嶇敓鏁堢増鏈彿

2. rule_version
瑙勫垯鐗堟湰涓庡弬鏁拌〃銆?
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
寤鸿绱㈠紩锛?
CREATE INDEX idx_rule_version_effective
ON rule_version(rule_code, enabled, effective_from, effective_to);
parameter_json 绀轰緥锛?
{
  "maxDelayMinutes": 30,
  "minDepositCount24h": 1,
  "minWithdrawCount24h": 1
}
璇存槑锛?
    鈥?瑙勫垯閫昏緫鍦?Java 涓?
    鈥?闃堝€笺€佸垎鏁般€佸惎鐢ㄧ姸鎬佸湪 DB 涓?
    鈥?effective_from / effective_to 鏀寔鏃堕棿鐢熸晥绐楀彛
    鈥?change_note 渚夸簬瀹¤

3. rule_hit_log
瑙勫垯鍛戒腑瀹¤琛ㄣ€?
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
寤鸿绱㈠紩锛?
CREATE INDEX idx_rule_hit_log_account_time
ON rule_hit_log(account_id, hit_time DESC);
CREATE INDEX idx_rule_hit_log_rule_time
ON rule_hit_log(rule_code, hit_time DESC);
evaluation_mode 寤鸿鍊硷細
    鈥?BATCH
    鈥?REALTIME
evidence_json 绀轰緥锛?
{
  "deposit_count_24h": 2,
  "withdraw_count_24h": 2,
  "withdraw_after_deposit_delay_avg_24h": 18.0,
  "threshold_max_delay_minutes": 30
}

4. rule_evaluation_job
鎵归噺瑙勫垯鎵ц浠诲姟琛ㄣ€?
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
鐘舵€佸缓璁細
    鈥?RUNNING
    鈥?SUCCESS
    鈥?FAILED
    鈥?PARTIAL_SUCCESS

5. rule_config_reload_log
閰嶇疆鐑姞杞芥棩蹇椼€?
CREATE TABLE rule_config_reload_log (
    reload_id             BIGSERIAL PRIMARY KEY,
    reload_time           TIMESTAMP NOT NULL,
    status                VARCHAR(32) NOT NULL,
    loaded_rule_count     INT,
    error_message         TEXT
);
鐢ㄩ€旓細
    鈥?璁板綍 JVM 缂撳瓨鍒锋柊
    鈥?鎺掓煡鈥滀负浠€涔堢嚎涓婅繕娌＄敓鏁堚€?
浜屻€佹帹鑽愬垵濮嬭鍒欐暟鎹?
寤鸿鍦ㄥ垵濮嬪寲鑴氭湰涓厛鎻掑叆涓€鎵规牳蹇冭鍒欍€?

1. rule_definition 鍒濆鍖栫ず渚?
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

2. rule_version 鍒濆鍖栫ず渚?
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

涓夈€乫raud-rule-engine 妯″潡 AI 浠ｇ爜鐢熸垚 Prompt
涓嬮潰鏄竴濂楀彲鐩存帴鐢ㄤ簬鐢熸垚浠ｇ爜鐨?Prompt锛屽缓璁寜椤哄簭閫愪釜浣跨敤锛岃€屼笉鏄竴娆℃€у叏鐢熸垚銆?

Prompt 1 鈥?鐢熸垚 fraud-rule-engine 椤圭洰楠ㄦ灦
You are a senior Java backend architect.
Generate a production-ready module named `fraud-rule-engine`.
Environment:
- Java 25
- Spring Boot 4.x
- Maven
- Spring JDBC
- PostgreSQL
Constraints:
- Do NOT use JPA, Hibernate, MyBatis, Drools, or expression engines.
- Keep dependencies minimal.
- Rule logic must remain in Java code.
- Rule parameters and versions must be loaded from PostgreSQL.
- Use explicit SQL only.
Responsibilities of this module:
- core rule interfaces
- rule evaluation context
- rule evaluation result
- rule engine
- rule registry
- rule config service
- rule version service
- rule result aggregator
- repositories for rule_definition, rule_version, rule_hit_log, rule_evaluation_job, rule_config_reload_log
Project package structure:
fraud-rule-engine
 鈹溾攢鈹€ api
 鈹溾攢鈹€ model
 鈹溾攢鈹€ engine
 鈹溾攢鈹€ config
 鈹溾攢鈹€ repository
 鈹斺攢鈹€ service
Generate:
- pom.xml
- package structure
- starter configuration
- all source files

Prompt 2 鈥?鐢熸垚鏍稿績妯″瀷涓庢帴鍙?
Generate the core Java models and interfaces for `fraud-rule-engine`.
Required enums:
- RuleCategory
- RuleSeverity
- EvaluationMode
- RuleJobStatus
Required models:
- RuleDefinition
- RuleVersion
- RuleConfig
- RuleEvaluationContext
- RuleEvaluationResult
- RuleEngineResult
- RuleEvaluationJob
- RuleConfigReloadLog
Required interface:
- FraudRule
Requirements:
- Plain Java, no Lombok unless truly necessary
- Use record where suitable
- Include field-level clarity
- Ensure all models map cleanly to PostgreSQL tables

Prompt 3 鈥?鐢熸垚 JDBC Repositories
Generate Spring JDBC repositories for `fraud-rule-engine`.
Repositories required:
- RuleDefinitionRepository
- RuleVersionRepository
- RuleHitLogRepository
- RuleEvaluationJobRepository
- RuleConfigReloadLogRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- Explicit SQL only
- Implement RowMapper classes
- Provide:
  - save
  - update
  - findByRuleCode
  - findActiveRules
  - findEffectiveVersions
  - insertRuleHit
  - createJob
  - updateJobStatus

Prompt 4 鈥?鐢熸垚 RuleConfigService 涓庣儹鍔犺浇鏈哄埗
Generate RuleConfigService and related classes.
Responsibilities:
- Load active rule versions from PostgreSQL
- Cache rule configs in JVM memory
- Refresh configs periodically
- Expose getConfig(ruleCode, asOfTime)
- Expose reload()
Requirements:
- Keep implementation simple
- No Redis required
- Use scheduled refresh
- Write reload results to rule_config_reload_log
- Handle malformed parameter_json safely

Prompt 5 鈥?鐢熸垚 RuleEngine 涓?RuleRegistry
Generate RuleRegistry, RuleEngine, RuleResultAggregator, and RuleEvaluationService.
Responsibilities:
- Register available FraudRule implementations
- Select active rules
- Evaluate rules for a RuleEvaluationContext
- Aggregate hits, score, and reason codes
- Persist rule hit logs when rules hit
Requirements:
- RuleEngine must not contain business-specific rule logic
- Keep orchestration readable
- Support both BATCH and REALTIME evaluation modes
- Return a RuleEngineResult

Prompt 6 鈥?鐢熸垚 fraud-rule-engine 椤圭洰楠ㄦ灦
Generate a second module named `fraud-rule-engine`.
Environment:
- Java 25
- Maven
Purpose:
Provide concrete rule implementations using the core engine APIs.
Dependencies:
- depend on fraud-rule-engine
- depend on fraud-core
- depend on fraud-feature models where needed
Package structure:
fraud-rule-engine
 鈹溾攢鈹€ login
 鈹溾攢鈹€ transaction
 鈹溾攢鈹€ device
 鈹溾攢鈹€ security
 鈹溾攢鈹€ graph
 鈹斺攢鈹€ composite
Generate:
- pom.xml
- package structure
- all source files

Prompt 7 鈥?鐢熸垚绗竴鎵瑰叿浣撹鍒欑被
Generate the following rule classes in `fraud-rule-engine`:
Login rules:
- HighRiskIpLoginRule
- LoginFailureBurstRule
Transaction rules:
- RapidWithdrawAfterDepositRule
- RewardWithdrawAbuseRule
Device rules:
- SharedDeviceClusterRule
- DeviceSwitchSpikeRule
Security rules:
- RapidProfileChangeRule
- SecurityChangeBeforeWithdrawRule
Graph rules:
- HighRiskNeighborClusterRule
Composite rules:
- AtoSuspicionRule
Requirements:
- Each rule implements FraudRule
- Each rule reads feature values from AccountFeatureSnapshot
- Each rule reads thresholds from RuleConfig
- Each rule must return:
  - ruleCode
  - hit boolean
  - severity
  - score
  - reasonCode
  - message
  - evidence map
  - ruleVersion
- Keep logic simple and explicit

Prompt 8 鈥?鐢熸垚瑙勫垯鍙傛暟瑙ｆ瀽鍣?
Generate parameter parsing classes for the rule engine.
Purpose:
Convert rule_version.parameter_json into typed config objects.
Requirements:
- One typed config object per major rule or rule family
- Safe parsing with validation
- Reject malformed config early
- Keep the JSON schema simple
- No heavy serialization frameworks beyond Jackson if already used

Prompt 9 鈥?鐢熸垚 Rule Engine 瀵瑰鏈嶅姟閫傞厤灞?
Generate a RuleEngineFacade service.
Responsibilities:
- Evaluate rules for a single account
- Evaluate rules for batch accounts
- Accept AccountFeatureSnapshot and optional graph/context signals
- Return RuleEngineResult
- Persist hit logs
- Expose a clean API for fraud-risk and fraud-case modules
Methods:
- evaluateAccount(accountId, context)
- evaluateBatch(accountIds, contexts)
Requirements:
- Make it easy for other modules to consume
- Hide repository/config orchestration details

Prompt 10 鈥?鐢熸垚瑙勫垯鍒濆鍖?SQL 涓?README
Generate:
1. SQL initialization scripts for rule_definition and rule_version
2. README for fraud-rule-engine and fraud-rule-engine
README should include:
- architecture overview
- how rule logic is structured
- how rule configs are stored
- how rule versioning works
- how rule hot reload works
- how to add a new rule
- how BATCH and REALTIME evaluation differ

Prompt 11 鈥?鐢熸垚 Rule Engine 娴嬭瘯
Generate unit tests and focused integration tests for the rule engine.
Test cases required:
- HighRiskIpLoginRule hit / no-hit
- RapidWithdrawAfterDepositRule hit / no-hit
- SharedDeviceClusterRule hit / no-hit
- AtoSuspicionRule hit / no-hit
- RuleConfigService reload behavior
- RuleEngine batch evaluation
- Rule hit persistence
Constraints:
- Keep tests stable
- Avoid unnecessary test frameworks
- Use realistic feature snapshots

鍥涖€佹帹鑽愮涓€闃舵涓婄嚎瑙勫垯
绗竴闃舵寤鸿鍏堝仛杩?8 鏉★紝澶熺敤涓斾环鍊奸珮锛?
    1. HIGH_RISK_IP_LOGIN
    2. LOGIN_FAILURE_BURST
    3. RAPID_WITHDRAW_AFTER_DEPOSIT
    4. REWARD_WITHDRAW_ABUSE
    5. SHARED_DEVICE_CLUSTER
    6. RAPID_PROFILE_CHANGE
    7. SECURITY_CHANGE_BEFORE_WITHDRAW
    8. ATO_SUSPICION_COMPOSITE
杩欑粍瑙勫垯宸茬粡鑳借鐩栵細
    鈥?鎾炲簱
    鈥?鐩楀彿
    鈥?缇婃瘺鍏?
    鈥?鍥紮鍏变韩璁惧
    鈥?鎻愮幇椋庨櫓

浜斻€佷笌鍏朵粬妯″潡鐨勪緷璧栧叧绯?
寤鸿渚濊禆鏂瑰悜濡備笅锛?
fraud-feature
    鈫?
fraud-rule-engine
    鈫?
fraud-rule-engine
    鈫?
fraud-risk
    鈫?
fraud-case
鍏抽敭鐐癸細
    鈥?fraud-rule-engine 璇诲彇 Feature Store
    鈥?涓嶇洿鎺ヤ緷璧?Python ML
    鈥?fraud-risk 鍙互鎶婅鍒欏垎鍜?ML 鍒嗚瀺鍚?
    鈥?fraud-case 鍜?fraud-ai 鐩存帴娑堣垂瑙勫垯缁撴灉

鍏€佽惤鍦板缓璁?
浣犲綋鍓嶆渶鍚堢悊鐨勬帹杩涢『搴忔槸锛?
    1. feature_store_schema.sql
    2. fraud-feature
    3. rule_engine_schema.sql
    4. fraud-rule-engine
    5. fraud-rule-engine
    6. fraud-risk
杩欐牱涓€鏃?Rule Engine 钀藉湴锛屼綘灏卞凡缁忔湁涓€濂楀彲瑙ｉ噴銆佸彲瀹¤銆佸彲涓婄嚎鐨勫弽娆鸿瘓鏍稿績銆?
涓嬩竴姝ユ渶鑷劧鐨勬槸缁х画琛ヤ笂锛?
fraud-risk 鐨勭敓浜х骇璁捐 + schema + AI 浠ｇ爜鐢熸垚 Prompt銆
