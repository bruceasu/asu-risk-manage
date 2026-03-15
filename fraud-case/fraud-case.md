生产级 fraud-case 设计，包含三部分：
    1. fraud-case 模块设计
    2. case_management_schema.sql
    3. fraud-case AI 代码生成 Prompt
这一层的目标是把前面各层输出收拢成一个可审计、可复核、可供 AI 报告消费的结构化案件对象。
它承接：
    • AccountFeatureSnapshot
    • RuleEngineResult
    • GraphRiskSignal
    • RiskScoreResult
    • 可选 MlAnomalySignal
并输出：
    • InvestigationCase
    • CaseSummary
    • Timeline
    • RecommendedActions

一、fraud-case 的定位
fraud-case 不是简单存一个风险分。
它是反欺诈系统的调查对象构建层。
负责：
    • 将多个检测结果整理成统一案件
    • 保存证据摘要
    • 保存关键时间线
    • 生成调查建议
    • 为人工审核和 AI 报告提供稳定输入
一句话：
fraud-case 是风险结果到调查对象之间的桥梁。

二、核心设计原则
2.1 Case 必须结构化
不要只存一段描述文本。
至少要有：
    • 基本信息
    • 风险摘要
    • 规则命中
    • 图谱摘要
    • 特征摘要
    • 时间线
    • 建议动作

2.2 Case 必须可追溯
要能回答：
    • 当时的特征版本是什么
    • 命中了哪些规则，版本是多少
    • 风险分是多少
    • 使用了哪个评分 profile
    • 由哪次任务生成

2.3 Case 不应直接依赖原始海量日志
Case 应该依赖：
    • 上游结果
    • 少量关键事件摘要
而不是每次 AI 报告都去扫描全部原始事件。

2.4 Case 要适配离线和实时
Phase-1：
    • 批量生成高风险账户案件
Phase-2：
    • 事件驱动生成/更新案件

三、模块职责
建议模块：
fraud-case
├── model
├── builder
├── recommendation
├── timeline
├── repository
├── service
└── job

四、输入输出设计

4.1 输入
基础输入
    • accountId
    • AccountProfile
    • AccountFeatureSnapshot
风险输入
    • RiskScoreResult
规则输入
    • RuleEngineResult
图输入
    • GraphRiskSignal
ML 输入（可选）
    • MlAnomalySignal
事件输入（精选）
    • 最近 N 条关键事件，用于构造时间线

4.2 输出
建议主输出对象：
InvestigationCase
建议字段：
    • caseId
    • accountId
    • caseStatus
    • riskSummary
    • featureSummary
    • ruleSummary
    • graphSummary
    • timeline
    • recommendedActions
    • generatedAt
    • featureVersion
    • evaluationMode

五、Case 数据结构设计

5.1 RiskSummary
public record RiskSummary(
    double riskScore,
    String riskLevel,
    String profileName,
    List<String> topReasonCodes,
    ScoreBreakdown scoreBreakdown
) {}

5.2 FeatureSummary
这里只保留对调查最重要的特征，不必塞满全部 100+ 字段。
建议包括：
    • accountAgeDays
    • highRiskIpLoginCount24h
    • loginFailureRate24h
    • newDeviceLoginCount7d
    • withdrawAfterDepositDelayAvg24h
    • sharedDeviceAccounts7d
    • securityChangeBeforeWithdrawFlag24h
    • graphClusterSize30d
    • riskNeighborCount30d
    • anomalyScoreLast

5.3 RuleSummary
public record RuleSummary(
    int totalRuleScore,
    List<RuleEvaluationResult> hits
) {}

5.4 GraphSummary
public record GraphSummary(
    double graphScore,
    int graphClusterSize,
    int riskNeighborCount,
    int sharedDeviceAccounts,
    int sharedBankAccounts
) {}

5.5 TimelineEvent
public record TimelineEvent(
    Instant eventTime,
    String eventType,
    String title,
    String description,
    Map<String, Object> evidence
) {}

5.6 RecommendedAction
建议动作枚举：
    • MONITOR
    • MANUAL_REVIEW
    • ENHANCED_VERIFICATION
    • WITHDRAWAL_DELAY
    • TEMP_LIMIT
    • FREEZE_RECOMMENDATION
第一版只做建议，不直接做执行。

六、Case Builder 设计
核心组件：
InvestigationCaseBuilder
负责把多个输入组装成案件。
建议分三个子组件：
    • RiskSummaryBuilder
    • TimelineBuilder
    • RecommendationService

6.1 RiskSummaryBuilder
从 RiskScoreResult 构建风险摘要。

6.2 TimelineBuilder
从关键事件中生成时间线。
第一版建议只取与风险相关的事件：
    • 登录
    • 密码修改
    • 2FA 变更
    • 入金
    • 提现
    • 关键转账
按时间排序后生成 timeline。

6.3 RecommendationService
根据规则和风险等级输出建议动作。
例如：
    • CRITICAL + 提现相关规则 → WITHDRAWAL_DELAY
    • HIGH + ATO suspicion → ENHANCED_VERIFICATION
    • MEDIUM → MANUAL_REVIEW
    • LOW → MONITOR

七、Timeline 生成策略
Timeline 非常重要，因为调查员和 AI 都需要它。
建议 Timeline 不是全量事件，而是精选关键节点。

7.1 关键事件优先级
优先纳入：
    1. 高风险登录
    2. 新设备登录
    3. 密码重置 / 安全资料修改
    4. 入金
    5. 提现
    6. 大额转账
    7. 奖励到账
    8. 收口账户转移

7.2 示例 Timeline
2025-03-14 02:14 login from new device
2025-03-14 02:16 password changed
2025-03-14 02:21 deposit completed
2025-03-14 02:39 withdrawal initiated
这样 AI 很容易生成调查摘要。

八、建议动作设计
建议动作要稳定且可解释。

8.1 动作规则建议
LOW
    • MONITOR
MEDIUM
    • MANUAL_REVIEW
HIGH
    • MANUAL_REVIEW
    • ENHANCED_VERIFICATION
CRITICAL
    • WITHDRAWAL_DELAY
    • ENHANCED_VERIFICATION
    • TEMP_LIMIT

8.2 组合条件
例如：
    • 命中 SECURITY_CHANGE_BEFORE_WITHDRAW
    • 命中 ATO_SUSPICION_COMPOSITE
    • riskLevel = CRITICAL
则推荐：
    • WITHDRAWAL_DELAY
    • ENHANCED_VERIFICATION

九、Case 状态设计
建议状态：
    • OPEN
    • UNDER_REVIEW
    • ESCALATED
    • CLOSED_CONFIRMED_FRAUD
    • CLOSED_FALSE_POSITIVE
    • CLOSED_MONITOR_ONLY
第一版可以先只做：
    • OPEN
    • UNDER_REVIEW
    • CLOSED_*

十、case_management_schema.sql
下面给出推荐表结构。

1. investigation_case
主案件表。
CREATE TABLE investigation_case (
    case_id                  BIGSERIAL PRIMARY KEY,
    account_id               VARCHAR(64) NOT NULL,
    case_status              VARCHAR(64) NOT NULL,
    risk_score               DOUBLE PRECISION NOT NULL,
    risk_level               VARCHAR(32) NOT NULL,
    profile_name             VARCHAR(64) NOT NULL,
    top_reason_codes         TEXT,
    feature_version          INT NOT NULL,
    evaluation_mode          VARCHAR(32) NOT NULL,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL
);
索引：
CREATE INDEX idx_investigation_case_account
ON investigation_case(account_id);
CREATE INDEX idx_investigation_case_status
ON investigation_case(case_status);
CREATE INDEX idx_investigation_case_risk_level
ON investigation_case(risk_level);

2. case_risk_summary
详细风险摘要表。
CREATE TABLE case_risk_summary (
    case_id                  BIGINT PRIMARY KEY,
    score_breakdown_json     TEXT NOT NULL,
    rule_score               DOUBLE PRECISION,
    graph_score              DOUBLE PRECISION,
    anomaly_score            DOUBLE PRECISION,
    behavior_score           DOUBLE PRECISION,
    created_at               TIMESTAMP NOT NULL
);

3. case_feature_summary
关键特征摘要表。
CREATE TABLE case_feature_summary (
    case_id                                  BIGINT PRIMARY KEY,
    account_age_days                         INT,
    high_risk_ip_login_count_24h             INT,
    login_failure_rate_24h                   DOUBLE PRECISION,
    new_device_login_count_7d                INT,
    withdraw_after_deposit_delay_avg_24h     DOUBLE PRECISION,
    shared_device_accounts_7d                INT,
    security_change_before_withdraw_flag_24h BOOLEAN,
    graph_cluster_size_30d                   INT,
    risk_neighbor_count_30d                  INT,
    anomaly_score_last                       DOUBLE PRECISION,
    created_at                               TIMESTAMP NOT NULL
);

4. case_rule_hit
案件关联规则命中表。
CREATE TABLE case_rule_hit (
    case_rule_hit_id         BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    rule_code                VARCHAR(128) NOT NULL,
    rule_version             INT NOT NULL,
    severity                 VARCHAR(32) NOT NULL,
    score                    INT NOT NULL,
    reason_code              VARCHAR(128) NOT NULL,
    message                  TEXT,
    evidence_json            TEXT,
    created_at               TIMESTAMP NOT NULL
);
索引：
CREATE INDEX idx_case_rule_hit_case
ON case_rule_hit(case_id);

5. case_graph_summary
CREATE TABLE case_graph_summary (
    case_id                  BIGINT PRIMARY KEY,
    graph_score              DOUBLE PRECISION,
    graph_cluster_size       INT,
    risk_neighbor_count      INT,
    shared_device_accounts   INT,
    shared_bank_accounts     INT,
    created_at               TIMESTAMP NOT NULL
);

6. case_timeline_event
时间线表。
CREATE TABLE case_timeline_event (
    timeline_event_id        BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    event_time               TIMESTAMP NOT NULL,
    event_type               VARCHAR(64) NOT NULL,
    title                    VARCHAR(256) NOT NULL,
    description              TEXT,
    evidence_json            TEXT,
    created_at               TIMESTAMP NOT NULL
);
索引：
CREATE INDEX idx_case_timeline_event_case_time
ON case_timeline_event(case_id, event_time ASC);

7. case_recommended_action
建议动作表。
CREATE TABLE case_recommended_action (
    case_action_id           BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    action_code              VARCHAR(64) NOT NULL,
    action_reason            TEXT,
    created_at               TIMESTAMP NOT NULL
);

8. case_generation_job
批量案件生成任务表。
CREATE TABLE case_generation_job (
    job_id                   BIGSERIAL PRIMARY KEY,
    job_type                 VARCHAR(32) NOT NULL,
    started_at               TIMESTAMP NOT NULL,
    finished_at              TIMESTAMP,
    status                   VARCHAR(32) NOT NULL,
    target_account_count     INT,
    processed_account_count  INT,
    failed_account_count     INT,
    error_message            TEXT
);

十一、Java 模型建议

11.1 InvestigationCase
public record InvestigationCase(
    Long caseId,
    String accountId,
    String caseStatus,
    RiskSummary riskSummary,
    FeatureSummary featureSummary,
    RuleSummary ruleSummary,
    GraphSummary graphSummary,
    List<TimelineEvent> timeline,
    List<String> recommendedActions,
    Instant createdAt,
    Instant updatedAt,
    int featureVersion,
    String evaluationMode
) {}

11.2 FeatureSummary
public record FeatureSummary(
    int accountAgeDays,
    int highRiskIpLoginCount24h,
    double loginFailureRate24h,
    int newDeviceLoginCount7d,
    double withdrawAfterDepositDelayAvg24h,
    int sharedDeviceAccounts7d,
    boolean securityChangeBeforeWithdrawFlag24h,
    int graphClusterSize30d,
    int riskNeighborCount30d,
    double anomalyScoreLast
) {}

11.3 RecommendedActionDecision
如果你想更清晰一些，可以用对象而不是字符串。
public record RecommendedActionDecision(
    String actionCode,
    String actionReason
) {}

十二、Repository 设计
建议以下 Repository：
    • InvestigationCaseRepository
    • CaseRiskSummaryRepository
    • CaseFeatureSummaryRepository
    • CaseRuleHitRepository
    • CaseGraphSummaryRepository
    • CaseTimelineEventRepository
    • CaseRecommendedActionRepository
    • CaseGenerationJobRepository
全部用 Spring JDBC。

十三、fraud-case AI 代码生成 Prompt
下面这套 Prompt 可直接用于生成代码。

Prompt 1 — 生成模块骨架
You are a senior Java backend architect.
Generate a production-ready module named `fraud-case`.
Environment:
- Java 25
- Spring Boot 4.x
- Maven
- Spring JDBC
- PostgreSQL
Constraints:
- Do NOT use JPA, Hibernate, or MyBatis.
- Use explicit SQL only.
- Keep dependencies minimal.
- Keep case objects audit-friendly and structured.
Responsibilities:
- build structured investigation cases
- persist case summary, rule hits, graph summary, timeline, and recommended actions
- support batch and realtime case generation
- provide retrieval APIs for other internal modules
Project structure:
fraud-case
 ├── model
 ├── builder
 ├── recommendation
 ├── timeline
 ├── repository
 ├── service
 └── job
Generate:
- pom.xml
- package structure
- all source files

Prompt 2 — 生成模型类
Generate Java model classes for `fraud-case`.
Required classes:
- InvestigationCase
- RiskSummary
- FeatureSummary
- RuleSummary
- GraphSummary
- TimelineEvent
- RecommendedActionDecision
- CaseGenerationJob
Requirements:
- Use plain Java
- Use record where suitable
- Include field-level clarity
- Align with PostgreSQL schema

Prompt 3 — 生成 JDBC Repositories
Generate Spring JDBC repositories for `fraud-case`.
Repositories required:
- InvestigationCaseRepository
- CaseRiskSummaryRepository
- CaseFeatureSummaryRepository
- CaseRuleHitRepository
- CaseGraphSummaryRepository
- CaseTimelineEventRepository
- CaseRecommendedActionRepository
- CaseGenerationJobRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- Explicit SQL only
- Implement RowMapper classes where needed
- Provide save(), batchInsert(), findByCaseId(), findLatestByAccountId()

Prompt 4 — 生成 Builder 组件
Generate the core builder components for `fraud-case`.
Required classes:
- InvestigationCaseBuilder
- RiskSummaryBuilder
- FeatureSummaryBuilder
- RuleSummaryBuilder
- GraphSummaryBuilder
Responsibilities:
- accept AccountFeatureSnapshot
- accept RuleEngineResult
- accept GraphRiskSignal
- accept RiskScoreResult
- optionally accept MlAnomalySignal
- build an InvestigationCase
Requirements:
- keep mappings explicit
- avoid hidden transformations
- ensure outputs are stable and audit-friendly

Prompt 5 — 生成 TimelineBuilder
Generate TimelineBuilder for `fraud-case`.
Responsibilities:
- build a concise timeline from recent key events
- prioritize:
  - login events
  - new device login
  - password reset
  - security changes
  - deposits
  - withdrawals
  - high-value transfers
- sort by event time ascending
Output:
- list of TimelineEvent
Requirements:
- do not include every raw event
- produce a clean investigation-oriented timeline

Prompt 6 — 生成 RecommendationService
Generate RecommendationService for `fraud-case`.
Responsibilities:
- recommend actions based on:
  - risk level
  - top reason codes
  - selected rule hits
- possible actions:
  - MONITOR
  - MANUAL_REVIEW
  - ENHANCED_VERIFICATION
  - WITHDRAWAL_DELAY
  - TEMP_LIMIT
  - FREEZE_RECOMMENDATION
Requirements:
- keep recommendation logic explicit
- avoid hidden heuristics
- return both action code and reason

Prompt 7 — 生成 Case Service
Generate CaseService and CaseFacade for `fraud-case`.
Responsibilities:
- create case for one account
- create cases in batch
- persist all case-related tables
- retrieve latest case by account
- retrieve full case detail by caseId
Methods:
- createCase(...)
- createBatchCases(...)
- getLatestCaseByAccountId(...)
- getCaseDetailByCaseId(...)
Requirements:
- support BATCH and REALTIME generation modes
- keep orchestration readable
- use transactions where appropriate

Prompt 8 — 生成 Batch Job Runner
Generate CaseGenerationJobRunner.
Responsibilities:
- process target accounts in batches
- build investigation cases
- persist results
- update case_generation_job table
- log progress
Requirements:
- default batch size 500 or 1000
- do not load large datasets fully into memory
- include error handling

Prompt 9 — 生成初始化 SQL 与 README
Generate:
1. SQL initialization scripts for any required lookup or default case state data
2. README for `fraud-case`
README should include:
- architecture overview
- case object structure
- how timeline is built
- how recommendations are generated
- batch vs realtime case generation
- how the module supports AI reporting

Prompt 10 — 生成测试
Generate unit tests and focused integration tests for `fraud-case`.
Test cases required:
- build case from sample inputs
- timeline generation correctness
- recommendation generation correctness
- persistence of case summary and detail tables
- latest case retrieval by accountId
- batch case generation flow
Requirements:
- keep tests stable
- use realistic sample risk/rule/feature inputs

十四、第一阶段建议
第一阶段 fraud-case 不要做太多 workflow 功能。
先做：
    • 生成案件
    • 存案件
    • 查询案件
    • 时间线
    • 推荐动作
先不要做：
    • 审批流
    • 评论系统
    • 多审核员协作
    • 自动关闭策略
这些都属于第二阶段甚至第三阶段。

十五、与 AI 报告的关系
fraud-case 是 fraud-ai 的最佳输入。
AI 层不应该直接读原始事件，而应该优先读：
    • InvestigationCase
    • RuleSummary
    • Timeline
    • RecommendedActions
这样你会得到：
    • 更稳定的报告
    • 更一致的解释
    • 更好的审计能力
