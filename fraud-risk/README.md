# fraud-risk

`fraud-risk` 是统一风险评分模块，负责把规则、图、ML、行为信号汇总成最终 `RiskScoreResult`。

它当前最适合的定位是：

- 核心 `lib`
- 保留轻量 `app` 外壳
- 对外稳定入口是 [RiskEngineFacade.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEngineFacade.java)

## 当前边界

### 对外入口

- [RiskEngineFacade.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEngineFacade.java)
  - 给 `offline`、批量 job、未来在线接入提供统一评分入口

### 编排层

- [RiskEvaluationService.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEvaluationService.java)
  - 统一评估单账户和批量账户风险
  - 当前只负责编排，不再承担太多口径细节

### 评分与信号组件

- [GraphRiskSignalResolver.java](fraud-risk/src/main/java/me/asu/ta/risk/service/GraphRiskSignalResolver.java)
  - 统一图信号解析与回退
- [BehaviorScoreCalculator.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScoreCalculator.java)
  - 生成 `BehaviorRiskSignal`
- [BehaviorScorePolicy.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScorePolicy.java)
  - 行为评分阈值、分值、原因码集中定义
- [RiskScoreCalculator.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/RiskScoreCalculator.java)
  - 最终分数公式
- [RiskReasonGenerator.java](fraud-risk/src/main/java/me/asu/ta/risk/reason/RiskReasonGenerator.java)
  - top reason codes 合并与排序
- [RiskScoreResultFactory.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskScoreResultFactory.java)
  - 最终结果对象装配

## 输入信号

`fraud-risk` 当前消费 4 类输入：

- 规则信号
  - [RuleEngineResult.java](fraud-rule-engine/src/main/java/me/asu/ta/rule/model/RuleEngineResult.java)
- 图信号
  - [GraphRiskSignal.java](fraud-risk/src/main/java/me/asu/ta/risk/model/GraphRiskSignal.java)
- ML 信号
  - [MlAnomalySignal.java](fraud-risk/src/main/java/me/asu/ta/risk/model/MlAnomalySignal.java)
- 行为信号
  - [BehaviorRiskSignal.java](fraud-risk/src/main/java/me/asu/ta/risk/model/BehaviorRiskSignal.java)

## 图信号口径

当前图信号统一由 [GraphRiskSignalResolver.java](fraud-risk/src/main/java/me/asu/ta/risk/service/GraphRiskSignalResolver.java) 处理。

当 `RiskEvaluationRequest.graphRiskSignal` 为空时，会基于 `AccountFeatureSnapshot` 回退构造图信号。

这套口径同时用于：

- `RiskEvaluationService`
- `RiskEvaluationJobRunner`
- `RiskEngineFacade` 传给 `rule-engine` 的图上下文

这样规则引擎与风险评分对“无显式 graph 输入”的处理保持一致。

## 行为评分口径

当前行为评分由 [BehaviorScoreCalculator.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScoreCalculator.java) 执行，阈值与分值集中在 [BehaviorScorePolicy.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScorePolicy.java)。

当前已纳入的行为信号包括：

- `loginFailureRate24h`
- `highRiskIpLoginCount24h`
- `withdrawAfterDepositDelayAvg24h`
- `sharedDeviceAccounts7d`
- `securityChangeBeforeWithdrawFlag24h`
- 离线上下文：
  - `behaviorClusterSize`
  - `similarAccountCount`
  - `coordinatedTradingScore`

## 原因码生成

[RiskReasonGenerator.java](fraud-risk/src/main/java/me/asu/ta/risk/reason/RiskReasonGenerator.java) 当前已经按来源拆分内部逻辑：

- rule reason candidates
- graph reason candidates
- ml reason candidates
- behavior reason candidates

但对外仍保持统一入口。

## 权重与风险等级

默认 profile：

- `DEFAULT`
  - `0.40 * rule + 0.25 * graph + 0.20 * anomaly + 0.15 * behavior`
- `NO_ML`
  - `0.55 * rule + 0.30 * graph + 0.00 * anomaly + 0.15 * behavior`

风险等级由 [RiskLevelClassifier.java](fraud-risk/src/main/java/me/asu/ta/risk/classification/RiskLevelClassifier.java) 判定。

## 批量评估

- [RiskEvaluationJobRunner.java](fraud-risk/src/main/java/me/asu/ta/risk/job/RiskEvaluationJobRunner.java)
  - 负责批量账户风险评估
  - 默认每批 `1000`

## 与其他模块的关系

### 调用 `rule-engine`

`fraud-risk` 通过 [RiskEngineFacade.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEngineFacade.java) 调用 [RuleEngineFacade.java](fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleEngineFacade.java)。

### 被 `offline` 调用

`fraud-offline` 通过 [OfflineRiskBatchService.java](fraud-offline/src/main/java/me/asu/ta/offline/integration/OfflineRiskBatchService.java) 调用 `RiskEngineFacade`。

### 被 `case` 复用结果

`fraud-case` 会消费统一风险结果，并在需要时直接调用 `rule-engine` 获取规则命中。

## 当前建议

- 保留 `RiskEngineFacade`
- 不建议继续新增更多薄 facade
- 继续把 `fraud-risk` 维持在“评分核心 lib”而不是“总控程序”

## 验证

最近一轮 `fraud-risk` 收口已通过：

```bash
mvn -pl fraud-risk -am clean test -Dmaven.test.skip=false -DskipTests=false
mvn verify
```
