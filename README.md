# asu-risk-manage

`asu-risk-manage` 是一个面向交易风控与欺诈分析的多模块 Maven 工程。
它把离线回放、实时分析、特征沉淀、规则评估、图谱信号、统一风险评分、案件生成和 AI 报告组织在同一套体系内，方便按模块独立演进，也方便按链路联调。

## 项目目标

项目当前重点解决三类问题：

- 离线分析
  - 基于交易与报价文件做 replay、markout、quote age、baseline、异常检测和聚类分析。
- 在线风控
  - 对实时事件流做轻量分析、风险打分和规则触发。
- 统一风险闭环
  - 将特征、规则、图谱、异常和行为信号汇总为统一风险结果，并沉淀为案件与 AI 调查报告。

## 顶层架构

仓库按“能力模块”而不是“技术层”拆分，顶层模块如下：

| 模块 | 作用 |
| --- | --- |
| `fraud-core` | 公共 DTO、统计工具、基础模型与通用算法 |
| `fraud-offline` | 离线批处理入口，负责文件输入、回放分析、辅助文件输出，以及可选接入当前体系 |
| `fraud-online` | 实时风控演示与事件回放入口 |
| `fraud-feature` | 账户特征快照、历史特征与特征存储能力 |
| `fraud-rule-engine` | 规则评估、命中结果与规则侧证据 |
| `fraud-risk` | 统一风险评分、风险等级、原因码与批量评估编排 |
| `fraud-graph` | 关系边构建、连通簇分析、图谱信号与图风险摘要 |
| `fraud-case` | 将风险结果整理为可审计、可调查的案件对象 |
| `fraud-ai` | 基于案件对象生成结构化 AI 调查报告 |
| `data-generator` | 示例数据与数据生成辅助内容 |
| `docs` | 补充设计说明、参考文档和专题资料 |

聚合工程定义见 [pom.xml](/d:/03_projects/suk/asu-risk-manage/pom.xml)。

## 典型数据流

### 1. 离线批处理链路

1. `fraud-offline` 读取 `trades.csv / quotes.csv`
2. 完成 replay、markout、quote age、baseline、异常和聚类分析
3. 生成辅助文件，如 `markout_detail.csv`、`baseline.csv`、`risk_report.txt`
4. 如启用当前体系集成模式，再映射为：
   - `AccountFeatureSnapshot`
   - `MlAnomalySignal`
   - `GraphRiskSignal`
5. 调用 `fraud-feature` 与 `fraud-risk`，统一落入快照、历史与风险结果

### 2. 在线风控链路

1. `fraud-online` 消费实时事件或文件回放事件
2. 生成账户侧实时统计与轻量风险信号
3. 与规则、特征、图谱或后续风控引擎对接

### 3. 调查闭环链路

1. `fraud-risk` 产出统一风险结果
2. `fraud-case` 组装案件主对象、时间线、规则命中和建议动作
3. `fraud-ai` 基于案件对象生成结构化调查报告

## 当前模块关系

可以把系统理解为下面这条主线：

`offline / online -> feature / rule / graph -> risk -> case -> ai`

其中：

- `fraud-core` 是横向公共能力，不直接承载业务编排。
- `fraud-offline` 现在更像“离线入口和编排层”，而不是独立评分中心。
- `fraud-risk` 是统一风险口径主落点。
- `fraud-case` 和 `fraud-ai` 负责把机器结果转成可审计、可阅读、可行动的输出。

## 快速开始

### 环境要求

- JDK `25`
- Maven `3.9+`
- 建议使用仓库内的本地 settings：
  - `.mvn-local-settings.xml`

Windows 下可先执行：

```powershell
.\setup-jdk25.ps1
```

### 编译整个工程

```bash
mvn -s .mvn-local-settings.xml clean compile
```

### 运行离线分析

```bash
java -cp fraud-offline/target/classes me.asu.ta.FxReplayPlus ^
  --trades data/trades.csv ^
  --quotes data/quotes.csv ^
  --out-dir out ^
  --agg-account ^
  --baseline ^
  --report ^
  --cluster
```

如需把离线结果并入当前体系：

```bash
java -cp fraud-offline/target/classes me.asu.ta.FxReplayPlus ^
  --trades data/trades.csv ^
  --quotes data/quotes.csv ^
  --out-dir out ^
  --agg-account ^
  --baseline ^
  --report ^
  --cluster ^
  --integrate-current-system
```

### 运行实时演示

当前实时演示入口位于：

- [RealtimeRiskEngine.java](/d:/03_projects/suk/asu-risk-manage/fraud-online/src/src/main/java/me/asu/ta/RealtimeRiskEngine.java)

更详细的编译与运行方式见：

- [fraud-online/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-online/README.md)

## 重点子模块入口

Spring Boot 模块入口：

- [FraudFeatureApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-feature/src/main/java/me/asu/ta/feature/FraudFeatureApplication.java)
- [FraudRuleEngineCoreApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-rule-engine/src/main/java/me/asu/ta/rule/FraudRuleEngineCoreApplication.java)
- [FraudRiskApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-risk/src/main/java/me/asu/ta/risk/FraudRiskApplication.java)
- [FraudGraphApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-graph/src/main/java/me/asu/ta/graph/FraudGraphApplication.java)
- [FraudCaseApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-case/src/main/java/me/asu/ta/casemanagement/FraudCaseApplication.java)
- [FraudAiApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-ai/src/main/java/me/asu/ta/ai/FraudAiApplication.java)

离线入口：

- [FxReplayPlus.java](/d:/03_projects/suk/asu-risk-manage/fraud-offline/src/main/java/me/asu/ta/FxReplayPlus.java)
- [OfflineReplayCliApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-offline/src/main/java/me/asu/ta/offline/OfflineReplayCliApplication.java)
- [OfflineClusterCliApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-offline/src/main/java/me/asu/ta/offline/OfflineClusterCliApplication.java)
- [OfflineReportCliApplication.java](/d:/03_projects/suk/asu-risk-manage/fraud-offline/src/main/java/me/asu/ta/offline/OfflineReportCliApplication.java)

## 子模块文档

建议按关注点继续阅读：

- [fraud-offline/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-offline/README.md)
- [fraud-risk/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-risk/README.md)
- [fraud-graph/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-graph/README.md)
- [fraud-case/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-case/README.md)
- [fraud-ai/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-ai/README.md)
- [docs/README.md](/d:/03_projects/suk/asu-risk-manage/docs/README.md)
- [data-generator/README.md](/d:/03_projects/suk/asu-risk-manage/data-generator/README.md)

## 输出与落点

当前系统同时支持两类结果：

- 辅助文件输出
  - 主要由 `fraud-offline` 生成，用于人工排查、回放验证和分析。
- 体系内结构化结果
  - 主要包括特征快照、风险结果、图谱信号、案件对象和 AI 报告。

推荐理解方式：

- 文件是“分析材料”
- 快照、风险、案件和报告是“系统主结果”

## 构建与验证

常用命令：

```bash
mvn -s .mvn-local-settings.xml clean compile
mvn verify
```

当前仓库有一个已知问题会影响全量 `verify`：

- [fraud-core/src/test/java/me/asu/AppTest.java](/d:/03_projects/suk/asu-risk-manage/fraud-core/src/test/java/me/asu/AppTest.java)
  - 当前环境存在读取权限异常，会导致 reactor 在 `fraud-core:testCompile` 提前失败。

因此在问题修复前，建议把验证拆成两层：

- 模块级编译验证
  - 例如 `mvn -pl fraud-offline -am compile`
- 定向测试验证
  - 按具体模块执行对应测试

## 当前约束

- 本仓库当前以最小依赖、显式 SQL、可审计逻辑为主，不引入重型图数据库或复杂 ORM。
- `fraud-offline` 的部分离线明细仍以辅助文件形式暴露，不强行写入现有 schema。
- `fraud-online` 当前更偏演示与验证入口，不等同于完整在线生产接入层。
- 个别子模块 README 仍有历史编码遗留问题，根 README 以当前代码结构为准。

## 推荐阅读顺序

如果你是第一次接触这个项目，建议这样读：

1. 先看本文件，建立整体模块视图
2. 再看 [fraud-offline/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-offline/README.md) 和 [fraud-risk/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-risk/README.md)
3. 如果关注关系分析，继续看 [fraud-graph/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-graph/README.md)
4. 如果关注调查闭环，继续看 [fraud-case/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-case/README.md) 和 [fraud-ai/README.md](/d:/03_projects/suk/asu-risk-manage/fraud-ai/README.md)
