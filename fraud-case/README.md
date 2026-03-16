# fraud-case

`fraud-case` 负责把特征、规则、图谱和风险评估结果整理成可审计、可检索、适合人工调查与 AI 报告消费的结构化案件对象。

## Architecture Overview

模块分层如下：

- `model`
  承载案件主对象、摘要对象、时间线、推荐动作和批任务对象。
- `builder`
  负责把 `AccountFeatureSnapshot`、`RuleEngineResult`、`RiskScoreResult` 等输入显式映射成案件相关对象。
- `timeline`
  负责从关键风险信号生成精简调查时间线。
- `recommendation`
  负责根据风险等级、关键原因码和规则命中生成建议动作。
- `repository`
  使用 Spring JDBC + 显式 SQL 持久化案件主表和各明细表。
- `service`
  编排单账号建案、批量建案、案件查询和对外 facade。
- `job`
  负责按批次扫描账号并生成案件。

当前实现主要复用上游模块：

- `fraud-feature` 提供 `AccountFeatureSnapshot`
- `fraud-rule-engine` 提供规则评估结果
- `fraud-risk` 提供最终风险分、风险等级和原因码

## Case Object Structure

一个完整案件由主案表和多张明细表组成：

- `investigation_case`
  保存账号、风险分、风险等级、profile、top reason codes、feature version、evaluation mode 等主信息。
- `case_risk_summary`
  保存 `score_breakdown_json` 以及 rule / graph / anomaly / behavior 四类分数。
- `case_feature_summary`
  保存调查最关键的特征字段，而不是全部快照字段。
- `case_rule_hit`
  保存命中的规则、版本、严重级别、分值、原因码和证据。
- `case_graph_summary`
  保存图谱相关摘要。
- `case_timeline_event`
  保存精简调查时间线。
- `case_recommended_action`
  保存建议动作及原因。

对内聚合读取时，模块通过 `InvestigationCaseBundle` 返回完整案件视图。

## How Timeline Is Built

时间线由 [CaseTimelineBuilder.java]fraud-case/src/main/java/me/asu/ta/casemanagement/timeline/CaseTimelineBuilder.java) 生成，目标是“调查导向”，不是原始日志回放。

当前优先纳入的事件类型包括：

- `CASE_CREATED`
- `RULE_HIT`
- `LOGIN_PATTERN`
- `NEW_DEVICE_LOGIN`
- `PASSWORD_RESET`
- `SECURITY_PATTERN`
- `DEPOSIT_ACTIVITY`
- `WITHDRAWAL_ACTIVITY`
- `HIGH_VALUE_TRANSFER`

实现特点：

- 只使用当前已聚合好的特征快照和规则结果，不扫描全量原始事件表
- 对调查最相关的模式做摘要化输出
- 最终按 `event_time` 升序排序
- 证据字段统一序列化为 JSON，便于审计和 AI 消费

## How Recommendations Are Generated

推荐动作由 [CaseRecommendationBuilder.java]fraud-case/src/main/java/me/asu/ta/casemanagement/recommendation/CaseRecommendationBuilder.java) 生成，逻辑保持显式，不使用隐藏启发式。

当前主要依据：

- `riskLevel`
- `topReasonCodes`
- 关键规则命中/图谱暴露
- 关键特征标志，例如 `securityChangeBeforeWithdrawFlag24h`

默认动作逻辑大致如下：

- `CRITICAL`
  - `FREEZE_ACCOUNT`
  - `MANUAL_REVIEW`
- `HIGH`
  - `HOLD_WITHDRAWAL`
  - `STEP_UP_VERIFICATION`
- `MEDIUM`
  - `MONITOR_ACCOUNT`
- `LOW`
  - `RETAIN_FOR_AUDIT`

附加条件还会补充：

- `REVIEW_SECURITY_CHANGES`
- `INVESTIGATE_LINKED_ACCOUNTS`

## Batch vs Realtime Case Generation

### Realtime

实时建案入口由 [CaseService.java]fraud-case/src/main/java/me/asu/ta/casemanagement/service/CaseService.java) 和 [CaseFacade.java]fraud-case/src/main/java/me/asu/ta/casemanagement/service/CaseFacade.java) 提供。

典型流程：

1. 读取账号最新特征快照
2. 执行规则评估
3. 执行风险评估
4. 构建案件主对象、摘要、时间线、建议动作
5. 在事务边界内持久化所有案件相关表

### Batch

批量建案由 [CaseGenerationJobRunner.java]fraud-case/src/main/java/me/asu/ta/casemanagement/job/CaseGenerationJobRunner.java) 驱动。

特点：

- 默认批大小 `1000`
- 使用 `account_feature_snapshot` 做 keyset 风格分页读取
- 不会一次把全量账号载入内存
- 当前改为“账号级独立处理”
  - 单个账号失败不会阻断其他账号
  - 全部跑完但有失败时，job 状态为 `PARTIAL_SUCCESS`

## How The Module Supports AI Reporting

`fraud-case` 的目标之一，是给 AI 报告层提供稳定、结构化、审计友好的输入，而不是让 AI 直接扫描原始事件。

适合 AI 消费的内容包括：

- `investigation_case`
  主结论、风险等级、主要原因码
- `case_risk_summary`
  分数拆解，便于解释为什么高风险
- `case_rule_hit`
  命中规则和证据
- `case_timeline_event`
  精简时间线，便于生成调查叙事
- `case_recommended_action`
  建议动作和理由

这种结构能让 AI 输出更稳定：

- 不依赖海量原始日志
- 不容易遗漏关键信号
- 解释链路更清晰
- 更容易追溯和审计

## Reference SQL

模块当前没有单独的 lookup 表。  
为便于初始化、联调和运营核对，参考 SQL 放在：

- [01_case_reference_data.sql]fraud-case/sql/01_case_reference_data.sql)

它提供了当前代码里使用的：

- case status 参考值
- job status 参考值
- recommendation action code 参考值
- timeline event type 参考值
