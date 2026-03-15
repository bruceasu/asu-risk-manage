下面继续，给你一套 生产级 fraud-risk 设计，包含三部分：
    1. fraud-risk 模块设计
    2. risk_engine_schema.sql
    3. fraud-risk AI 代码生成 Prompt
目标是把前面这些信号统一收口：
    • Rule Engine Result
    • Graph Signals
    • ML Anomaly Score（可选）
    • Behavior / statistical signals
并输出：
    • 最终风险分
    • 风险等级
    • 风险原因
    • 评分拆解
    • 可审计记录
这层是整个反欺诈系统的决策聚合层。

一、fraud-risk 的定位
fraud-risk 不做原始检测。
它的职责是：
    • 接收多个检测子系统输出
    • 进行统一标准化
    • 计算最终风险分
    • 生成风险等级
    • 产出 reason codes
    • 持久化风险评估结果
一句话：
fraud-risk 是反欺诈系统的评分与决策编排器。

二、核心设计原则
2.1 风险引擎不重算底层逻辑
它不应该自己再去查原始事件表做判断。
它只消费上游结果：
    • AccountFeatureSnapshot
    • RuleEngineResult
    • GraphRiskSignal
    • MlAnomalySignal

2.2 评分逻辑必须显式
不要把评分逻辑藏进模型或复杂脚本。
第一版推荐：
    • 显式公式
    • 明确权重
    • 明确阈值
例如：
final_score =
0.40 * rule_score +
0.25 * graph_score +
0.20 * anomaly_score +
0.15 * behavior_score
如果没有 ML：
final_score =
0.55 * rule_score +
0.30 * graph_score +
0.15 * behavior_score

2.3 风险输出必须可解释
最终输出不能只有一个数字。
至少要包含：
    • risk_score
    • risk_level
    • reason_codes
    • score_breakdown
    • top_evidence

2.4 支持 Phase-1 到 Phase-2 演进
Phase-1 主要是 batch 风险评估。
Phase-2 再扩展到 event-driven risk scoring。

三、模块职责
建议模块名：
fraud-risk
内部建议拆分逻辑包：
fraud-risk
├── model
├── scoring
├── classification
├── reason
├── repository
├── service
└── job

四、输入输出设计

4.1 输入
Rule 输入
来自 fraud-rule-engine
RuleEngineResult
包含：
    • totalRuleScore
    • hits
    • reasonCodes

Graph 输入
来自 fraud-graph
建议统一成：
GraphRiskSignal
字段可包含：
    • graphScore
    • clusterSize
    • riskNeighborCount
    • sharedDeviceAccounts
    • sharedBankAccounts

ML 输入
来自 Python ML Service
建议统一成：
MlAnomalySignal
字段：
    • anomalyScoreRaw
    • anomalyScoreNormalized
    • modelName
    • scoredAt

Behavior 输入
来自 Feature Store
例如：
    • login_failure_rate_24h
    • withdraw_after_deposit_delay_avg_24h
    • night_login_ratio_7d
这部分可以先抽成：
BehaviorRiskSignal

4.2 输出
统一输出：
RiskScoreResult
建议字段：
    • accountId
    • riskScore
    • riskLevel
    • reasonCodes
    • scoreBreakdown
    • generatedAt
    • featureVersion
    • ruleVersionSummary
    • evaluationMode

五、评分模型设计

5.1 分层评分结构
建议先将各子系统结果标准化为 0-100。
Rule Score
规则引擎可直接输出 0-100 范围，或通过 capped sum 标准化。
Graph Score
图信号通过简单规则映射到 0-100。
Anomaly Score
Python ML 输出 raw score，再归一化到 0-100。
Behavior Score
对少量关键行为特征做规则化评分。

5.2 第一版推荐公式
有 ML 时
final_score =
0.40 * rule_score +
0.25 * graph_score +
0.20 * anomaly_score +
0.15 * behavior_score
无 ML 时
final_score =
0.55 * rule_score +
0.30 * graph_score +
0.15 * behavior_score

5.3 风险等级建议
区间	风险等级
0–29	LOW
30–59	MEDIUM
60–79	HIGH
80–100	CRITICAL
后续可按业务调。

六、Behavior Score 设计
这部分是很多系统容易忽略的，但非常有用。
第一版可只取 5–8 个关键特征，做一个轻量评分器。
例如：
    • login_failure_rate_24h
    • high_risk_ip_login_count_24h
    • withdraw_after_deposit_delay_avg_24h
    • shared_device_accounts_7d
    • security_change_before_withdraw_flag_24h
    • graph_cluster_size_30d
示意：
if login_failure_rate_24h > 0.8 -> +15
if high_risk_ip_login_count_24h >= 1 -> +20
if withdraw_after_deposit_delay_avg_24h <= 30 -> +20
if shared_device_accounts_7d >= 5 -> +20
if security_change_before_withdraw_flag_24h = true -> +25
cap at 100
这比一开始上复杂统计模型更稳。

七、风险原因生成
fraud-risk 需要把上游原因做统一归纳。
例如：
    • Rule Engine 提供 reason_codes
    • Graph 提供 GRAPH_SHARED_DEVICE_CLUSTER
    • ML 提供 ML_ANOMALY_HIGH
fraud-risk 不一定自己创造新的原因，但要：
    • 统一去重
    • 排序
    • 选出 top reasons
建议输出：
    • primaryReasonCodes
    • secondaryReasonCodes

八、评分拆解设计
必须保留评分拆解，方便审计和解释。
建议模型：
ScoreBreakdown
- ruleScore
- graphScore
- anomalyScore
- behaviorScore
- finalScore
- weightingProfile

九、权重配置设计
权重不要硬编码在代码各处。
建议集中配置。
可用一张表或配置文件。
第一版建议先用 DB 表。

9.1 risk_weight_profile
一个系统可能有多种 profile：
    • DEFAULT
    • NO_ML
    • NEW_ACCOUNT
    • BUSINESS_ACCOUNT
第一版先做：
    • DEFAULT
    • NO_ML
后续再细分。

十、risk_engine_schema.sql
下面给出推荐表结构。

1. risk_weight_profile
CREATE TABLE risk_weight_profile (
    profile_name         VARCHAR(64) PRIMARY KEY,
    rule_weight          DOUBLE PRECISION NOT NULL,
    graph_weight         DOUBLE PRECISION NOT NULL,
    anomaly_weight       DOUBLE PRECISION NOT NULL,
    behavior_weight      DOUBLE PRECISION NOT NULL,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL
);
初始化示例：
INSERT INTO risk_weight_profile
(profile_name, rule_weight, graph_weight, anomaly_weight, behavior_weight, enabled, created_at, updated_at)
VALUES
('DEFAULT', 0.40, 0.25, 0.20, 0.15, TRUE, NOW(), NOW()),
('NO_ML',   0.55, 0.30, 0.00, 0.15, TRUE, NOW(), NOW());

2. risk_score_result
主风险评分结果表。
CREATE TABLE risk_score_result (
    score_id                BIGSERIAL PRIMARY KEY,
    account_id              VARCHAR(64) NOT NULL,
    risk_score              DOUBLE PRECISION NOT NULL,
    risk_level              VARCHAR(32) NOT NULL,
    profile_name            VARCHAR(64) NOT NULL,
    feature_version         INT NOT NULL,
    generated_at            TIMESTAMP NOT NULL,
    evaluation_mode         VARCHAR(32) NOT NULL,
    top_reason_codes        TEXT,
    score_breakdown_json    TEXT NOT NULL
);
索引：
CREATE INDEX idx_risk_score_result_account_time
ON risk_score_result(account_id, generated_at DESC);
CREATE INDEX idx_risk_score_result_level_time
ON risk_score_result(risk_level, generated_at DESC);
top_reason_codes 可用逗号拼接，或 JSON。
第一版用 text 即可。

3. risk_reason_mapping
标准化 reason code 映射表。
CREATE TABLE risk_reason_mapping (
    reason_code            VARCHAR(128) PRIMARY KEY,
    reason_title           VARCHAR(256) NOT NULL,
    reason_description     TEXT NOT NULL,
    severity               VARCHAR(32) NOT NULL,
    category               VARCHAR(64) NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL
);
初始化示例：
INSERT INTO risk_reason_mapping
(reason_code, reason_title, reason_description, severity, category, created_at, updated_at)
VALUES
('HIGH_RISK_IP_LOGIN', 'High Risk IP Login', 'Login from high risk IP detected', 'HIGH', 'LOGIN', NOW(), NOW()),
('RAPID_WITHDRAW_AFTER_DEPOSIT', 'Rapid Withdraw After Deposit', 'Withdrawal occurred shortly after deposit', 'HIGH', 'TRANSACTION', NOW(), NOW()),
('SHARED_DEVICE_CLUSTER', 'Shared Device Cluster', 'Account shares device with multiple accounts', 'HIGH', 'DEVICE', NOW(), NOW()),
('SECURITY_CHANGE_BEFORE_WITHDRAW', 'Security Change Before Withdraw', 'Security profile change detected shortly before withdrawal', 'HIGH', 'SECURITY', NOW(), NOW()),
('ML_ANOMALY_HIGH', 'ML Anomaly High', 'Anomaly model produced a high abnormality score', 'MEDIUM', 'ML', NOW(), NOW());

4. risk_evaluation_job
批量风险评估任务表。
CREATE TABLE risk_evaluation_job (
    job_id                  BIGSERIAL PRIMARY KEY,
    job_type                VARCHAR(32) NOT NULL,
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(32) NOT NULL,
    target_account_count    INT,
    processed_account_count INT,
    failed_account_count    INT,
    error_message           TEXT
);

十一、Java 模型设计
建议核心模型如下。

11.1 GraphRiskSignal
public record GraphRiskSignal(
    double graphScore,
    int graphClusterSize,
    int riskNeighborCount,
    int sharedDeviceAccounts,
    int sharedBankAccounts
) {}

11.2 MlAnomalySignal
public record MlAnomalySignal(
    double anomalyScoreRaw,
    double anomalyScoreNormalized,
    String modelName,
    Instant scoredAt
) {}

11.3 BehaviorRiskSignal
public record BehaviorRiskSignal(
    double behaviorScore,
    Map<String, Object> evidence
) {}

11.4 ScoreBreakdown
public record ScoreBreakdown(
    double ruleScore,
    double graphScore,
    double anomalyScore,
    double behaviorScore,
    double finalScore,
    String profileName
) {}

11.5 RiskScoreResult
public record RiskScoreResult(
    String accountId,
    double riskScore,
    String riskLevel,
    String profileName,
    int featureVersion,
    Instant generatedAt,
    String evaluationMode,
    List<String> topReasonCodes,
    ScoreBreakdown scoreBreakdown
) {}

十二、风险引擎组件设计
建议组件如下：
fraud-risk
├── model
├── scoring
├── classification
├── reason
├── repository
├── service
└── job

12.1 scoring
包含：
    • RiskScoreCalculator
    • BehaviorScoreCalculator
    • RiskWeightProfileService

12.2 classification
包含：
    • RiskLevelClassifier

12.3 reason
包含：
    • RiskReasonGenerator
    • RiskReasonMappingRepository

12.4 service
包含：
    • RiskEvaluationService
    • RiskEngineFacade

十三、BehaviorScoreCalculator 设计
第一版建议不要过度抽象，直接基于 Feature Store 关键字段做加分。
例如：
if (features.loginFailureRate24h() > 0.8) score += 15;
if (features.highRiskIpLoginCount24h() >= 1) score += 20;
if (features.withdrawAfterDepositDelayAvg24h() > 0
    && features.withdrawAfterDepositDelayAvg24h() <= 30) score += 20;
if (features.sharedDeviceAccounts7d() >= 5) score += 20;
if (Boolean.TRUE.equals(features.securityChangeBeforeWithdrawFlag24h())) score += 25;
最后：
score = Math.min(score, 100.0);

十四、RiskReasonGenerator 设计
输入：
    • RuleEngineResult
    • GraphRiskSignal
    • MlAnomalySignal
    • BehaviorRiskSignal
输出：
    • 去重 reason codes
    • 优先级排序
    • top N（建议 3–5 个）
排序建议：
    1. CRITICAL/HIGH severity 的 rule reasons
    2. graph cluster / collector reasons
    3. ML anomaly
    4. behavior reasons

十五、Phase-1 / Phase-2 设计差异

Phase-1（离线）
风险评估针对批量账户执行。
流程：
Feature Snapshot
    ↓
Rule Engine Result
    ↓
Graph Risk Signal
    ↓
(optional) ML Signal
    ↓
Risk Evaluation
    ↓
risk_score_result

Phase-2（实时）
在事件到达时，使用最新 snapshot + 当前事件 context 做快速风险评分。
流程：
Incoming Event
    ↓
load latest feature snapshot
    ↓
partial rule evaluation
    ↓
optional ML call
    ↓
risk evaluation
    ↓
real-time risk result / alert
fraud-risk 核心组件基本不变，只是调用入口变成 event-driven。

十六、fraud-risk AI 代码生成 Prompt
下面是可直接使用的 Prompt。

Prompt 1 — 生成模块骨架
You are a senior Java backend architect.
Generate a production-ready module named `fraud-risk`.
Environment:
- Java 25
- Spring Boot 4.x
- Maven
- Spring JDBC
- PostgreSQL
Constraints:
- Do NOT use JPA, Hibernate, MyBatis, or scripting engines.
- Keep dependencies minimal.
- Use explicit SQL only.
- Keep scoring logic transparent and explicit.
Responsibilities:
- aggregate rule, graph, ML, and behavior signals
- calculate final risk score
- classify risk level
- generate top reason codes
- persist risk evaluation results
- support batch and realtime evaluation modes
Project structure:
fraud-risk
 ├── model
 ├── scoring
 ├── classification
 ├── reason
 ├── repository
 ├── service
 └── job
Generate:
- pom.xml
- package structure
- all source files

Prompt 2 — 生成模型类
Generate Java model classes for `fraud-risk`.
Required classes:
- GraphRiskSignal
- MlAnomalySignal
- BehaviorRiskSignal
- ScoreBreakdown
- RiskScoreResult
- RiskWeightProfile
- RiskEvaluationJob
Requirements:
- Use plain Java
- Use record where suitable
- Include field-level clarity
- Align with PostgreSQL schema

Prompt 3 — 生成 JDBC Repositories
Generate Spring JDBC repositories for `fraud-risk`.
Repositories required:
- RiskWeightProfileRepository
- RiskScoreResultRepository
- RiskReasonMappingRepository
- RiskEvaluationJobRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- Explicit SQL only
- Implement RowMapper classes
- Provide:
  - findProfileByName()
  - saveRiskScoreResult()
  - findLatestRiskScoreByAccountId()
  - createJob()
  - updateJobStatus()
  - findReasonMapping()

Prompt 4 — 生成评分器
Generate scoring components for `fraud-risk`.
Required classes:
- RiskScoreCalculator
- BehaviorScoreCalculator
- RiskWeightProfileService
Requirements:
1. RiskScoreCalculator must combine:
   - rule score
   - graph score
   - anomaly score
   - behavior score
2. Support both:
   - DEFAULT profile
   - NO_ML profile
3. BehaviorScoreCalculator should score risk using selected feature fields:
   - login_failure_rate_24h
   - high_risk_ip_login_count_24h
   - withdraw_after_deposit_delay_avg_24h
   - shared_device_accounts_7d
   - security_change_before_withdraw_flag_24h
4. Keep formulas explicit.
5. Cap scores to 0-100.

Prompt 5 — 生成 RiskLevelClassifier 与 ReasonGenerator
Generate:
- RiskLevelClassifier
- RiskReasonGenerator
Requirements:
RiskLevelClassifier:
- LOW: 0-29
- MEDIUM: 30-59
- HIGH: 60-79
- CRITICAL: 80-100
RiskReasonGenerator:
- combine reason codes from rule engine, graph signals, ML signal, and behavior signal
- remove duplicates
- prioritize higher severity reasons
- output top 3-5 reason codes

Prompt 6 — 生成 RiskEvaluationService
Generate RiskEvaluationService and RiskEngineFacade.
Responsibilities:
- accept accountId
- accept RuleEngineResult
- accept GraphRiskSignal
- accept optional MlAnomalySignal
- read AccountFeatureSnapshot if needed
- compute final risk score
- classify risk level
- generate score breakdown
- generate top reason codes
- persist result
Methods required:
evaluateAccountRisk(...)
evaluateBatchRisk(...)
Requirements:
- support BATCH and REALTIME evaluation modes
- keep orchestration readable
- do not hide scoring logic

Prompt 7 — 生成批量任务 Runner
Generate RiskEvaluationJobRunner.
Responsibilities:
- process accounts in batches
- create risk_evaluation_job row
- evaluate risk for all target accounts
- persist results
- log progress
- update job status
Requirements:
- batch size 1000 by default
- avoid loading full dataset into memory
- include error handling

Prompt 8 — 生成初始化 SQL 与 README
Generate:
1. SQL initialization scripts for:
   - risk_weight_profile
   - risk_reason_mapping
2. README for `fraud-risk`
README should include:
- architecture overview
- input signals
- scoring formula
- behavior score logic
- risk level thresholds
- how ML is optional
- batch vs realtime mode
- how to add a new weight profile

Prompt 9 — 生成测试
Generate unit tests and focused integration tests for `fraud-risk`.
Test cases required:
- score calculation with DEFAULT profile
- score calculation with NO_ML profile
- risk level classification
- top reason code generation
- persistence of RiskScoreResult
- batch evaluation flow
Requirements:
- keep tests stable
- use realistic sample inputs

十七、第一阶段建议上线能力
第一阶段 fraud-risk 建议只支持：
    • DEFAULT
    • NO_ML
并先消费：
    • RuleEngineResult
    • GraphRiskSignal
    • BehaviorScoreCalculator
ML 可以先接入为可选。
这意味着即使 Python ML 还没上线，fraud-risk 仍可工作。

十八、与其他模块的关系
依赖顺序建议：
fraud-feature
    ↓
fraud-rule-engine
    ↓
fraud-graph
    ↓
fraud-risk
    ↓
fraud-case
    ↓
fraud-ai
    • fraud-risk 不直接依赖原始事件表
    • 主要依赖 Feature Store 和上游评分结果
    • fraud-case 消费 RiskScoreResult
    • fraud-ai 再把 case 转为报告

十九、建议的下一步
现在最自然的下一步有两个方向：
    1. fraud-case 生产级设计 + schema + AI Prompt
把规则结果、风险分、图信号整理成结构化调查案件。
    2. fraud-graph 生产级设计 + schema + AI Prompt
先把图关系和团伙检测补齐，再让风险引擎拿到更完整输入。
更推荐先做 fraud-case，因为它能立刻把系统结果转成可审阅对象。

From <https://chatgpt.com/c/69aac9bb-7fcc-8324-91c0-7baf79f259f7> 

