# asu-risk-manage

`asu-risk-manage` 是一个面向交易风控分析的多模块 Maven 工程，覆盖离线回放分析、特征生成、规则评估、统一风险评分、案件生成和 AI 报告。

当前主链路已经收敛为：

`offline / batch / online -> feature / rule-engine / graph -> risk -> case -> ai`

## 模块概览

| 模块 | 作用 |
| --- | --- |
| `fraud-core` | 公共 DTO、统计工具、共享模型与通用常量 |
| `fraud-offline` | 离线文件输入、replay、markout、行为分群、相似度分析、辅助报告 |
| `fraud-batch` | 定期批量任务编排入口，负责 feature/risk/case 主链路调度 |
| `fraud-online` | 查询 API、手动重算 API、事件驱动重算入口 |
| `fraud-feature` | 账户特征快照、历史特征、特征生成与存储 |
| `fraud-rule-engine` | 规则配置装载、规则执行、规则命中聚合与日志 |
| `fraud-risk` | 统一风险评分、原因码生成、风险结果持久化 |
| `fraud-graph` | 图边构建、图信号、图摘要 |
| `fraud-case` | 调查案件、时间线、推荐动作、案件摘要 |
| `fraud-ai` | AI 调查报告生成与解析 |
| `docs` | 架构说明、专题文档、规格草案 |

## 当前入口形态

### 1. `fraud-batch`

`fraud-batch` 是正式的批处理编排入口，适合定时任务或调度平台调用。

职责：
- 刷新特征
- 批量评估风险
- 触发案件生成或更新

关键入口：
- [FraudBatchApplication](fraud-batch/src/main/java/me/asu/ta/batch/FraudBatchApplication.java)
- [BatchOrchestratorService](fraud-batch/src/main/java/me/asu/ta/batch/service/BatchOrchestratorService.java)

### 2. `fraud-online`

`fraud-online` 是第一阶段的在线入口，负责查询和显式重算，不承担完整实时决策引擎职责。

职责：
- 查询账号最新风险与历史
- 查询规则命中、特征快照、案件摘要
- 手动触发单账号重算
- 接收关键事件触发风险重算

关键入口：
- [FraudOnlineApplication](fraud-online/src/main/java/me/asu/ta/online/FraudOnlineApplication.java)
- [AccountRiskController](fraud-online/src/main/java/me/asu/ta/online/controller/AccountRiskController.java)
- [EventRiskController](fraud-online/src/main/java/me/asu/ta/online/controller/EventRiskController.java)

### 3. `fraud-offline`

`fraud-offline` 保持独立，主要用于：
- 历史回放
- 行为分群和行为相似分析
- 补算与验证
- 生成辅助分析材料

它不是日常查询主入口。

## 当前架构重点

### `fraud-risk` 已收口为评分核心

`fraud-risk` 当前更适合看作核心 `lib`，同时保留轻量运行壳。

关键组件：
- [RiskEngineFacade](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEngineFacade.java)
  - 跨模块稳定评分入口
- [RiskEvaluationService](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEvaluationService.java)
  - 统一评分编排层
- [GraphRiskSignalResolver](fraud-risk/src/main/java/me/asu/ta/risk/service/GraphRiskSignalResolver.java)
  - 图信号统一解析
- [BehaviorScoreCalculator](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScoreCalculator.java)
  - 行为信号计算
- [RiskReasonGenerator](fraud-risk/src/main/java/me/asu/ta/risk/reason/RiskReasonGenerator.java)
  - top reasons 合并与排序
- [RiskScoreResultFactory](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskScoreResultFactory.java)
  - 风险结果装配

### `fraud-rule-engine` 是共享规则内核

`fraud-rule-engine` 当前定位是独立模块，不是系统主程序。

关键组件：
- [RuleEngineFacade](fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleEngineFacade.java)
- [RuleEngine](fraud-rule-engine/src/main/java/me/asu/ta/rule/engine/RuleEngine.java)
- [RuleResultAggregator](fraud-rule-engine/src/main/java/me/asu/ta/rule/engine/RuleResultAggregator.java)

### 行为分析结果目前来自 `fraud-offline`

当前已经支持：
- `behavior feature vector`
- `behavior cluster`
- `behavior similarity edge`

这些结果属于辅助风控信号，不等同于实体关系图。

## 典型调用链路

### 定期批量计算

1. 调度器调用 `fraud-batch`
2. `fraud-batch` 刷新 `AccountFeatureSnapshot`
3. `fraud-batch` 调用 `RiskEngineFacade`
4. `fraud-risk` 内部调用 `RuleEngineFacade`
5. 生成 `RiskScoreResult`
6. 触发 `fraud-case` 生成或更新案件

### 事件驱动重算

1. 事件进入 `fraud-online`
2. `fraud-online` 读取最新 snapshot 并补充事件上下文
3. 调用 `RiskEngineFacade`
4. 生成并返回新的风险结果

### 查询链路

1. 查询请求进入 `fraud-online`
2. 优先读取最新 `RiskScoreResult`
3. 补充 `AccountFeatureSnapshot`、规则命中和案件摘要
4. 返回账号风险总览或历史明细

### 离线分析链路

1. `fraud-offline` 读取交易与报价文件
2. 执行 replay、baseline、anomaly、行为分析
3. 可选接入 `feature / risk / case`
4. 输出辅助分析文件和报告

## 运行环境

- JDK 25
- Maven 3.9+
- Spring Boot 3.5.7

## 常用命令

```bash
mvn clean compile
mvn verify
```

### 启动批处理入口

```bash
mvn -pl fraud-batch -am spring-boot:run
```

### 启动在线 API

```bash
mvn -pl fraud-online -am spring-boot:run
```

### 运行离线 replay

```bash
java -cp fraud-offline/target/classes me.asu.ta.offline.OfflineReplayCliApplication ^
  --trades data/trades.csv ^
  --quotes data/quotes.csv ^
  --out-dir out ^
  --agg-account ^
  --baseline ^
  --report
```

## 当前注意事项

- 查询默认查落库结果，不隐式触发重算。
- `fraud-batch` 只负责编排，不重复实现评分逻辑。
- `fraud-online` 当前不承担完整 allow/review/block 决策引擎职责。
- `fraud-offline` 输出的行为分群和行为相似边属于辅助分析材料。
- 当前全仓 Spring Boot 版本线已统一到 `3.5.7`。

## 推荐阅读顺序

1. [README.md](README.md)
2. [架构.md](docs/架构.md)
3. [fraud-offline/README.md](fraud-offline/README.md)
4. [fraud-risk/README.md](fraud-risk/README.md)
5. [fraud-case/README.md](fraud-case/README.md)
