这份设计重点解决 8 个问题：
    1. 规则如何表示
    2. 规则如何执行
    3. 规则如何版本化
    4. 规则如何热更新
    5. 规则如何审计
    6. 规则如何解释
    7. 规则如何与 Feature Store / Risk Engine 对接
    8. 规则如何从离线平滑演进到实时

1. Rule Engine 的定位
在反欺诈系统里，Rule Engine 不是“写几个 if/else”。
它应该是一个规则决策子系统，负责：
    • 消费账户特征、事件上下文、图谱信号
    • 评估确定性规则
    • 输出命中的规则、严重级别、解释、证据
    • 形成规则分数
    • 给 Risk Engine 提供稳定输入
一句话定义：
Rule Engine 是反欺诈系统中最可解释、最可控、最容易快速迭代的检测层。

2. Rule Engine 的核心原则
2.1 规则要“可解释”
每条规则都必须能回答：
    • 为什么命中
    • 命中的阈值是什么
    • 用了哪些特征
    • 证据是什么

2.2 规则要“可审计”
你必须能追溯：
    • 哪个版本的规则命中了该账户
    • 当时的阈值是多少
    • 当时使用的特征版本是什么

2.3 规则要“可分层”
不要把所有规则混在一起。建议至少分：
    • 登录风险规则
    • 交易风险规则
    • 设备风险规则
    • 安全事件规则
    • 图谱规则
    • 组合规则

2.4 规则要“配置化，但不要过度 DSL 化”
第一版不要上复杂脚本引擎，不要上 Drools 这类重框架。
最稳的方案是：
    • Java 规则类
    • 规则元数据存数据库
    • 阈值参数配置化
    • 支持热加载配置
这样既稳定又灵活。

3. 生产级总体架构
Feature Store / Event Context / Graph Signals
                    ↓
             RuleEvaluationContext
                    ↓
                Rule Engine
     ┌──────────────┼──────────────┐
     ↓              ↓              ↓
 Rule Library   Rule Registry   Rule Config
     ↓              ↓              ↓
                Rule Results
                    ↓
          Rule Score + Reason Codes
                    ↓
                Risk Engine

4. 建议模块划分
建议拆成两个模块：
fraud-rule-engine-core
fraud-rule-engine-library

4.1 fraud-rule-engine-core
放框架能力：
    • FraudRule
    • RuleEvaluationContext
    • RuleEvaluationResult
    • RuleEngine
    • RuleRegistry
    • RuleConfigRepository
    • RuleVersionService
    • RuleResultAggregator

4.2 fraud-rule-engine-library
放具体规则实现：
    • RapidWithdrawAfterDepositRule
    • HighRiskIpLoginRule
    • SharedDeviceClusterRule
    • RapidProfileChangeRule
    • NewAccountHighActivityRule
    • CredentialStuffingPatternRule
    • AtoSuspicionRule
    • CollectorAccountRule

5. 规则模型设计

5.1 Rule 元数据
每条规则都应具备以下属性：
    • rule_code
    • rule_name
    • category
    • description
    • severity
    • enabled
    • version
    • score_weight
    • parameter_set
    • evidence_template
例如：
rule_code: RAPID_WITHDRAW_AFTER_DEPOSIT
category: TRANSACTION
severity: HIGH
version: 3
score_weight: 25

5.2 核心接口
建议核心接口非常简单：
public interface FraudRule {
    String ruleCode();
    RuleCategory category();
    RuleSeverity severity();
    RuleEvaluationResult evaluate(RuleEvaluationContext context);
}

5.3 RuleEvaluationContext
这是规则的统一输入。
建议包括：
    • accountId
    • AccountFeatureSnapshot
    • Optional<RecentEventContext>
    • Optional<GraphSignalContext>
    • asOfTime
    • featureVersion
    • ruleConfigMap
例如：
public record RuleEvaluationContext(
    String accountId,
    AccountFeatureSnapshot features,
    RecentEventContext recentEventContext,
    GraphSignalContext graphContext,
    Instant asOfTime,
    int featureVersion,
    Map<String, RuleConfig> ruleConfigs
) {}

5.4 RuleEvaluationResult
必须能表达：
    • 是否命中
    • 命中原因
    • 分值
    • 证据
    • 使用了哪些字段
建议设计：
public record RuleEvaluationResult(
    String ruleCode,
    boolean hit,
    RuleSeverity severity,
    int score,
    String reasonCode,
    String message,
    Map<String, Object> evidence,
    int ruleVersion
) {}

6. 规则执行模型
建议采用：
6.1 Rule Registry
负责注册规则实例：
Map<String, FraudRule>

6.2 Rule Engine
负责：
    • 获取启用规则
    • 顺序执行
    • 收集结果
    • 聚合分数

6.3 Rule Result Aggregator
输出：
    • ruleHits
    • ruleScore
    • reasonCodes
    • evidenceSummary

7. 规则分类设计

7.1 Login Rules
例子：
    • HIGH_RISK_IP_LOGIN
    • VPN_LOGIN_SPIKE
    • IMPOSSIBLE_TRAVEL
    • LOGIN_FAILURE_BURST

7.2 Transaction Rules
例子：
    • RAPID_WITHDRAW_AFTER_DEPOSIT
    • HIGH_FREQUENCY_SMALL_AMOUNT
    • REPEATED_AMOUNT_PATTERN
    • REWARD_WITHDRAW_ABUSE

7.3 Device Rules
例子：
    • SHARED_DEVICE_CLUSTER
    • NEW_DEVICE_LOGIN_AFTER_DORMANCY
    • DEVICE_SWITCH_SPIKE

7.4 Security Rules
例子：
    • RAPID_PROFILE_CHANGE
    • SECURITY_CHANGE_BEFORE_WITHDRAW
    • PASSWORD_RESET_SPIKE

7.5 Graph Rules
例子：
    • HIGH_RISK_NEIGHBOR_CLUSTER
    • COLLECTOR_ACCOUNT_PATTERN
    • SHARED_BANK_ACCOUNT_CLUSTER

7.6 Composite Rules
组合规则更有价值。
例如：
ATO 组合规则
    • 高风险IP登录
    • 新设备登录
    • 改密
    • 提现
羊毛党组合规则
    • 新账户
    • 共享设备
    • 奖励交易
    • 快速提现

8. 配置化设计
这是生产里非常关键的一部分。

8.1 不建议把规则逻辑完全存数据库
比如存成表达式语言再运行，这样会：
    • 调试难
    • 类型不安全
    • 出问题难排查

8.2 推荐做法：代码中存逻辑，数据库中存参数
例如规则类中写逻辑：
if (features.withdrawAfterDepositDelayAvg24h() <= config.maxDelayMinutes()
    && features.depositCount24h() > 0
    && features.withdrawCount24h() > 0) {
    ...
}
数据库中存：
    • maxDelayMinutes = 30
    • score = 25
    • enabled = true
这样就既安全又灵活。

9. 规则配置表设计
建议至少 3 张表。

9.1 rule_definition
定义规则主信息。
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

9.2 rule_version
记录版本。
CREATE TABLE rule_version (
    rule_code            VARCHAR(128) NOT NULL,
    version              INT NOT NULL,
    parameter_json       TEXT NOT NULL,
    score_weight         INT NOT NULL,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from       TIMESTAMP NOT NULL,
    effective_to         TIMESTAMP,
    created_at           TIMESTAMP NOT NULL,
    PRIMARY KEY(rule_code, version)
);
说明：
    • parameter_json 存阈值
    • effective_from / to 支持生效窗口
    • 回溯案件时可知道当时用哪个版本

9.3 rule_hit_log
记录命中情况。
CREATE TABLE rule_hit_log (
    hit_id               BIGSERIAL PRIMARY KEY,
    account_id           VARCHAR(64) NOT NULL,
    rule_code            VARCHAR(128) NOT NULL,
    rule_version         INT NOT NULL,
    hit_time             TIMESTAMP NOT NULL,
    score                INT NOT NULL,
    reason_code          VARCHAR(128) NOT NULL,
    evidence_json        TEXT,
    feature_version      INT NOT NULL
);
这是审计核心表。

10. 热加载设计
生产里你会需要修改：
    • 阈值
    • score weight
    • enabled/disabled
但不一定每次都发版。
所以需要“热加载配置”，但不是“热加载 Java 代码”。

10.1 推荐方案
    • 规则逻辑在代码里
    • 参数在 DB
    • 定时刷新配置缓存
例如每 30 秒或 1 分钟刷新一次 rule_version

10.2 RuleConfigService
提供：
    • getConfig(ruleCode)
    • reload()
    • getActiveConfigs(asOfTime)

10.3 缓存建议
可以先用 JVM 内存缓存，不一定上 Redis。

11. 规则版本管理
11.1 为什么必须版本化
如果某条规则从：
    • delay <= 60 minutes
改成：
    • delay <= 30 minutes
那历史案件解释会完全不同。

11.2 版本原则
以下场景必须升版本：
    • 阈值变化
    • 使用特征变化
    • reason code 变化
    • score weight 变化
    • 命中逻辑变化

12. 规则输出设计
Rule Engine 最终应输出一个聚合结果对象。
建议：
public record RuleEngineResult(
    String accountId,
    int totalRuleScore,
    List<RuleEvaluationResult> hits,
    List<String> reasonCodes,
    Map<String, Object> summaryEvidence
) {}
这个对象直接给：
    • fraud-risk
    • fraud-case
    • fraud-ai

13. 规则分数设计
规则分数最好是离散且稳定的，不要搞复杂模型化。
建议：
Severity	Score Range
LOW	5–10
MEDIUM	10–20
HIGH	20–40
CRITICAL	40–80
单条规则不要直接拉满 100。
最终由 fraud-risk 统一融合。

14. 组合规则设计
生产里价值最高的往往不是单规则，而是组合规则。

14.1 组合规则实现方式
不要用规则互相调用。
建议用单独的 composite rule：
例如：
AtoSuspicionRule
内部判断：
    • new_device_login_count_7d > 0
    • high_risk_ip_login_count_24h > 0
    • security_change_before_withdraw_flag_24h = true
命中后给一个更高价值 reason code。

14.2 好处
    • 可解释
    • 不会形成复杂依赖图
    • 更适合审计

15. 与 Feature Store 的关系
Rule Engine 应尽量只依赖：
    • AccountFeatureSnapshot
    • 少量事件上下文
    • 图信号
不要让规则自己再去大量查原始事件表重算逻辑。
否则：
    • 性能不稳定
    • 口径漂移
    • 难以解释
推荐原则：
规则优先用 Feature Store，少量实时事件作补充。

16. 与 Risk Engine 的关系
Rule Engine 不负责最终决策，只负责：
    • 规则命中
    • 规则分
    • 理由
    • 证据
Risk Engine 再融合：
    • rule score
    • graph score
    • anomaly score
    • behavior score

17. 与 AI 调查报告的关系
AI 报告最适合引用：
    • rule hits
    • rule evidence
    • reason codes
例如：
    账户命中了“共享设备集群”和“入金后快速提现”两条高风险规则，且共享设备关联 12 个账户，入金后平均 18 分钟发起提现。
所以 Rule Engine 输出必须对 AI 友好。

18. Phase-1 与 Phase-2 的差异

18.1 Phase-1（离线）
Rule Engine 处理对象是：
    • feature snapshot
    • batch account list
调用方式：
accounts batch
   ↓
feature snapshot
   ↓
rule engine
   ↓
rule_hit_log

18.2 Phase-2（实时）
Rule Engine 增加一个入口：
    • event-triggered evaluation
例如收到提现事件时：
    • 读取最新 snapshot
    • 加入当前事件 context
    • 评估部分高价值实时规则
这时可以支持两种评估模式：
    • BATCH
    • REALTIME

19. 推荐 Java 包结构
fraud-rule-engine
├── model
│   ├── RuleCategory.java
│   ├── RuleSeverity.java
│   ├── RuleEvaluationContext.java
│   ├── RuleEvaluationResult.java
│   └── RuleEngineResult.java
├── api
│   └── FraudRule.java
├── engine
│   ├── RuleEngine.java
│   ├── RuleRegistry.java
│   └── RuleResultAggregator.java
├── config
│   ├── RuleConfig.java
│   ├── RuleConfigService.java
│   └── RuleVersionService.java
├── repository
│   ├── RuleDefinitionRepository.java
│   ├── RuleVersionRepository.java
│   └── RuleHitLogRepository.java
├── service
│   └── RuleEvaluationService.java
└── library
    ├── login
    │   ├── HighRiskIpLoginRule.java
    │   ├── LoginFailureBurstRule.java
    │   └── ImpossibleTravelRule.java
    ├── transaction
    │   ├── RapidWithdrawAfterDepositRule.java
    │   ├── HighFrequencySmallAmountRule.java
    │   └── RewardWithdrawAbuseRule.java
    ├── device
    │   ├── SharedDeviceClusterRule.java
    │   └── DeviceSwitchSpikeRule.java
    ├── security
    │   ├── RapidProfileChangeRule.java
    │   └── SecurityChangeBeforeWithdrawRule.java
    ├── graph
    │   ├── HighRiskNeighborClusterRule.java
    │   └── CollectorAccountRule.java
    └── composite
        ├── AtoSuspicionRule.java
        └── PromotionAbuseCompositeRule.java

20. 推荐第一批上线规则
你第一版不需要 100 条规则。
建议先上 12 条高价值规则：
    1. HIGH_RISK_IP_LOGIN
    2. VPN_LOGIN_SPIKE
    3. LOGIN_FAILURE_BURST
    4. RAPID_WITHDRAW_AFTER_DEPOSIT
    5. HIGH_FREQUENCY_SMALL_AMOUNT
    6. REWARD_WITHDRAW_ABUSE
    7. SHARED_DEVICE_CLUSTER
    8. DEVICE_SWITCH_SPIKE
    9. RAPID_PROFILE_CHANGE
    10. SECURITY_CHANGE_BEFORE_WITHDRAW
    11. HIGH_RISK_NEIGHBOR_CLUSTER
    12. ATO_SUSPICION_COMPOSITE
这批已经足够构成强有力的第一版系统。

21. 数据表最小集
如果你要一个最小但正确的落地方案，Rule Engine 至少需要：
    • rule_definition
    • rule_version
    • rule_hit_log
这 3 张表就够起步。

22. 最终落地建议
你的 Rule Engine 第一版最稳的实现方式是：
“Java 类实现规则逻辑 + PostgreSQL 存规则元数据与参数 + JVM 内缓存热加载 + 规则命中日志审计”
它兼顾了：
    • 可解释
    • 可审计
    • 可热更新
    • 低依赖
    • 适合 Spring JDBC
能从离线平滑演进到实时