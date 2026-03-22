# fraud-offline

`fraud-offline` 是基于交易 CSV 和报价 CSV 的离线分析模块。

它现在有两种工作方式：

- 文件分析模式：继续输出 `markout_detail.csv`、`baseline.csv`、`risk_report.txt`、`bot_indicators.csv` 等文件，适合人工分析和回放验证。
- 当前体系集成模式：在保留文件输出的同时，把离线结果映射到现有 `fraud-feature / fraud-risk` 体系，统一写入特征快照、历史快照和风险评分结果。

## 1. 当前结构

当前实现已经从单一入口程序拆成了分层结构：

- `me.asu.ta.offline.OfflineReplayCliApplication`
  - 离线分析统一 CLI 入口。
- `me.asu.ta.offline`
  - CLI 与总编排。
- `me.asu.ta.offline.analysis`
  - replay、baseline、anomaly、bot、cluster 等分析服务。
- `me.asu.ta.offline.io`
  - CSV、报告、图表输出。
- `me.asu.ta.offline.integration`
  - 离线结果到当前体系的映射与批处理接入。

核心类职责：

- `ReplayEngine`
  - 读取 trades/quotes，完成历史回放和账户聚合。
- `OfflineReplayFacade`
  - 统一调度 replay、输出、可选集成。
- `OfflineSnapshotMappingService`
  - 把离线结果映射为 `AccountFeatureSnapshot`。
- `OfflineGraphBridgeService`
  - 把离线聚类结果转换为 `GraphRiskSignal` 口径。
- `OfflineRiskBatchService`
  - 调用现有 `FeatureStoreService` 和 `RiskEngineFacade` 完成入库和评分。

## 2. 输入文件

### trades.csv

建议字段：

```csv
account_id,symbol,side,exec_time_ms,size,orderSize,takeProfit,stopLoss,eventText
```

说明：

- `account_id`
  - 账户 ID
- `symbol`
  - 交易品种
- `side`
  - `BUY` / `SELL`
- `exec_time_ms`
  - 成交时间，Unix epoch 毫秒
- `size`
  - 成交数量
- `orderSize`
  - 可选，订单原始大小
- `takeProfit`
  - 可选，止盈价格
- `stopLoss`
  - 可选，止损价格
- `eventText`
  - 可选，扩展事件文本，用于 bot/客户端特征分析

### quotes.csv

```csv
symbol,quote_time_ms,bid,ask
```

说明：

- `quote_time_ms`
  - 报价时间，Unix epoch 毫秒
- `mid = (bid + ask) / 2`
  - markout 基于 mid 计算

## 3. 支持的分析能力

- Markout 分析
  - 100ms / 500ms / 1s / 5s
- Quote Age 统计
  - p50 / p90 / p99 / mean
- 账户聚合
  - account、account|symbol、time bucket
- Bot 指标
  - CV、entropy、TPSL、client IP / client type
- Baseline / Z-score / anomaly
- 账户聚类
  - threshold clustering / k-means
- 统一风险评分接入
  - 可选写入 `AccountFeatureSnapshot`、history、`RiskScoreResult`

## 4. CLI 用法

### 基础运行

```bash
java -cp target/classes me.asu.ta.offline.OfflineReplayCliApplication \
  --trades trades.csv \
  --quotes quotes.csv
```

### 完整文件分析

```bash
java -cp target/classes me.asu.ta.offline.OfflineReplayCliApplication \
  --trades trades.csv \
  --quotes quotes.csv \
  --out-dir out \
  --agg-account \
  --baseline \
  --report \
  --cluster \
  --quoteage-stats \
  --charts \
  --min-trades 10
```

### 集成到当前体系

```bash
java -cp target/classes me.asu.ta.offline.OfflineReplayCliApplication \
  --trades trades.csv \
  --quotes quotes.csv \
  --out-dir out \
  --agg-account \
  --baseline \
  --report \
  --cluster \
  --integrate-current-system
```

### 重要参数

- `--trades <file>`
  - 必填，交易文件
- `--quotes <file>`
  - 必填，报价文件
- `--out-dir <dir>`
  - 统一输出目录
- `--agg-account`
  - 输出账户级聚合
- `--baseline`
  - 输出全局 baseline
- `--report`
  - 输出文本风险报告
- `--cluster`
  - 输出账户聚类
- `--quoteage-stats`
  - 输出 quote age 统计
- `--charts`
  - 输出 HTML dashboard
- `--min-trades <N>`
  - 最小样本数过滤
- `--integrate-current-system`
  - 启用当前体系集成模式

## 5. 输出说明

### 文件产物

- `markout_detail.csv`
  - 逐笔明细，包含 markout、quote age、bot 指标
- `markout_agg_by_account_symbol.csv`
  - 按账户和品种聚合
- `markout_agg_by_account.csv`
  - 按账户聚合，包含风险分和等级
- `markout_time_buckets.csv`
  - 时间桶聚合
- `quote_age_stats.csv`
  - quote age 分位数
- `baseline.csv`
  - 全局 baseline
- `clusters.csv`
  - 聚类结果
- `bot_indicators.csv`
  - 账户级 bot 指标
- `risk_report.txt`
  - 离线异常摘要；如果开启集成模式，会追加统一风控结果摘要
- `fx_replay_dashboard.html`
  - 图表看板

### 当前体系集成结果

启用 `--integrate-current-system` 后，离线结果会进一步映射并写入：

- `account_feature_snapshot`
- `account_feature_history`
- `risk_score_result`

当前映射原则：

- 不改数据库 schema
- 不把 `cv / botScore / markout 明细` 强行塞进现有表
- 优先落入已有字段：
  - `transactionCount24h`
  - `uniqueCounterpartyCount24h`
  - `graphClusterSize30d`
  - `riskNeighborCount30d`
  - `anomalyScoreLast`

## 6. 集成模式说明

集成模式下，`fraud-offline` 仍然是批处理入口，但不再把离线自有 `risk_score_0_100` 当作系统主评分口径。

它会执行：

1. `replay`
2. 生成离线 anomaly 和 cluster 结果
3. 映射为 `AccountFeatureSnapshot`
4. 生成 `MlAnomalySignal` / `GraphRiskSignal`
5. 调用现有 `RiskEngineFacade`
6. 持久化统一风险结果

注意：

- 该模式依赖现有 Spring 数据源和相关表配置。
- 如果数据库、规则配置或 repository 环境不完整，集成模式会失败。
- 默认文件模式不受影响。

## 7. 构建与验证

### 编译

```bash
mvn -pl fraud-offline -am compile
```

### 测试

仓库当前存在一个已知问题：`fraud-core/src/test/java/me/asu/AppTest.java` 读取权限异常，会导致全仓库 `mvn verify` 提前失败。

因此当前建议区分两层验证：

- 模块编译验证
  - `mvn -pl fraud-offline -am compile`
- 定向离线测试
  - `ReplayEngineTest`
  - `BotIndicatorAnalysisServiceTest`
  - `FxReplayClustererTest`
  - `OfflineReplayFacadeIntegrationTest`
  - `OfflineSnapshotMappingServiceTest`
  - `OfflineGraphBridgeServiceTest`
  - `OfflineRiskBatchServiceTest`

## 8. 已知限制

- `markout / quote age / cv / botScore` 目前仍主要通过辅助文件暴露，不直接落库。
- graph 这轮是“signal 桥接”，不是直接替代 `fraud-graph` 的图构建持久化流程。
- 风险评分统一后，离线原始分数更多用于解释和辅助分析，不再建议作为主结果长期消费。

## 9. 推荐使用方式

如果你要做人审分析：

- 直接使用文件模式
- 看 `markout_detail.csv`、`bot_indicators.csv`、`risk_report.txt`

如果你要把离线分析结果并入系统：

- 启用 `--integrate-current-system`
- 同时保留 `--out-dir`
- 让文件产物作为辅助核查材料
