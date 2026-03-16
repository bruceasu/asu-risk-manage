
Prompt 1 — 生成 fraud-rule-engine-core 项目骨架
You are a senior Java backend architect.
Generate a production-ready module named `fraud-rule-engine-core`.
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
fraud-rule-engine-core
 ├── api
 ├── model
 ├── engine
 ├── config
 ├── repository
 └── service
Generate:
- pom.xml
- package structure
- starter configuration
- all source files

Prompt 2 — 生成核心模型与接口
Generate the core Java models and interfaces for `fraud-rule-engine-core`.
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

Prompt 3 — 生成 JDBC Repositories
Generate Spring JDBC repositories for `fraud-rule-engine-core`.
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

Prompt 4 — 生成 RuleConfigService 与热加载机制
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

Prompt 5 — 生成 RuleEngine 与 RuleRegistry
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

Prompt 6 — 生成 fraud-rule-engine-library 项目骨架
Generate a second module named `fraud-rule-engine-library`.
Environment:
- Java 25
- Maven
Purpose:
Provide concrete rule implementations using the core engine APIs.
Dependencies:
- depend on fraud-rule-engine-core
- depend on fraud-core
- depend on fraud-feature models where needed
Package structure:
fraud-rule-engine-library
 ├── login
 ├── transaction
 ├── device
 ├── security
 ├── graph
 └── composite
Generate:
- pom.xml
- package structure
- all source files

Prompt 7 — 生成第一批具体规则类
Generate the following rule classes in `fraud-rule-engine-library`:
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

Prompt 8 — 生成规则参数解析器
Generate parameter parsing classes for the rule engine.
Purpose:
Convert rule_version.parameter_json into typed config objects.
Requirements:
- One typed config object per major rule or rule family
- Safe parsing with validation
- Reject malformed config early
- Keep the JSON schema simple
- No heavy serialization frameworks beyond Jackson if already used

Prompt 9 — 生成 Rule Engine 对外服务适配层
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

Prompt 10 — 生成规则初始化 SQL 与 README
Generate:
1. SQL initialization scripts for rule_definition and rule_version
2. README for fraud-rule-engine-core and fraud-rule-engine-library
README should include:
- architecture overview
- how rule logic is structured
- how rule configs are stored
- how rule versioning works
- how rule hot reload works
- how to add a new rule
- how BATCH and REALTIME evaluation differ

Prompt 11 — 生成 Rule Engine 测试
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