# Offline FX Replay 工具

本工具用于基于交易和报价 CSV 数据重放并分析市场冲击（markout）、报价时效（quote age）、Bot 检测等多维度风险指标。Java 实现，主要位于 `src/main/java/me/asu/ta` 目录。

## 功能概述

- **Markout 分析**：计算不同时间窗口的方向化 markout（100ms/500ms/1s/5s）
- **Quote Age 统计**：分析报价时效性，包括 p50/p90/p99 等分位数
- **聚合分析**：支持按账户、品种、时间桶多维度聚合
- **Bot 检测**：智能识别可疑机器人交易行为（基于系数变异、熵、TP/SL 模式等）
- **基线和 Z-score**：全局基线统计和异常检测
- **聚类分析**：k-means 或阈值聚类，快速定位相似账户
- **风险评分**：统一 0-100 风险评分体系，分级标注
- **可视化**：可选生成交互式 HTML Dashboard 图表
- **多格式输出**：详细明细、聚合统计、时间桶、quote age、baseline、聚类、bot 指标、风险报告等 CSV/TXT/HTML

## 源代码结构

核心源文件（`src/main/java/me/asu/ta`）：

- `FxReplayPlus.java` — 主程序入口，CLI 参数解析和流程控制
- `FxReplayEngine.java` — 核心回放引擎，处理交易和报价数据，计算 markout 和聚合
- `FxReplayWriter.java` — 统一 CSV/TXT 输出模块，采用 CsvWriter 标准处理
- `FxReplayClusterer.java` — k-means 和阈值聚类算法实现
- `FxReplayCommon.java` — 公共工具和统计计算函数
- `FxReplayCharts.java` — HTML Dashboard 图表生成
- `BaselineStats.java` — 基线统计容器类，避免重复计算
- `ReplayState.java` — 回放过程中间结果容器
- `DetailRow.java` — 逐笔交易详细记录
- `Agg.java` — 聚合统计累加器
- `LongSamples.java` — 分位数采样（用于 quote age 统计）
- `OfflineAccountTracker.java` — 账户级 Bot 检测追踪器
- `IntervalStats.java` — 交易间隔统计

**核心依赖**：

- `com.csvreader.CsvWriter` — CSV 文件写入（自动转义、引号处理）
- `com.csvreader.CsvReader` — CSV 文件读取

## 输入文件格式

### trades.csv

交易记录 CSV，字段：

```
account_id, symbol, side, exec_time_ms, size, [orderSize], [takeProfit], [stopLoss], [eventText]
```

- `account_id` — 账户标识
- `symbol` — 交易品种
- `side` — 方向（BUY/SELL 或 B/S）
- `exec_time_ms` — 执行时间（Unix epoch 毫秒）
- `size` — 交易数量
- `orderSize`（可选）— 订单初始数量，用于 Bot 检测
- `takeProfit`（可选）— 止盈价格
- `stopLoss`（可选）— 止损价格
- `eventText`（可选）— 事件文本数据，用于高级 Bot 检测

### quotes.csv

行情记录 CSV，字段：

```
symbol, quote_time_ms, bid, ask
```

- `symbol` — 品种
- `quote_time_ms` — 报价时间（Unix epoch 毫秒）
- `bid` — 买价
- `ask` — 卖价

**注**：mid = (bid + ask) / 2，markout 基于 mid 价格计算

## 使用方法

### 编译

```bash
cd asu-trading-analysis
mvn clean install
cd asu-trading-analysis-offline
mvn compile
```

### 运行

```bash
# 基础命令
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades trades.csv \
  --quotes quotes.csv

# 完整功能示例
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades trades.csv \
  --quotes quotes.csv \
  --agg-account true \
  --baseline true \
  --report true \
  --min-trades 10
```

## 命令行选项

### 基本输入输出

- `--trades <file>` — 交易文件路径（**必需**）
- `--quotes <file>` — 报价文件路径（**必需**）
- `--out-detail <file>` — 详细 markout 输出，默认 `markout_detail.csv`
- `--out-agg <file>` — 按 account|symbol 聚合，默认 `markout_agg_by_account_symbol.csv`

### 账户聚合

- `--agg-account true|false` — 是否按 account 单独聚合，默认 `false`
- `--out-agg-account <file>` — 按 account 聚合输出，默认 `markout_agg_by_account.csv`
- `--min-trades <N>` — 最小交易数过滤（避免小样本偏差），默认 `0`

### 时间桶聚合

- `--time-bucket-min <N>` — 时间桶大小（分钟），默认 `0`（不分桶）
- `--bucket-by all|account|symbol|account_symbol` — 聚合粒度，默认 `all`
- `--out-bucket <file>` — 时间桶统计输出，默认 `markout_time_buckets.csv`

### Quote Age 统计

- `--quoteage-stats true|false` — 是否计算 quote age 统计，默认 `false`
- `--quoteage-scope all|account|symbol|account_symbol` — quote age 聚合范围，默认 `all`
- `--quoteage-max-samples <N>` — 每个 key 最大样本数，默认 `200000`
- `--out-quoteage <file>` — quote age 输出文件，默认 `quote_age_stats.csv`

### 基线和异常检测

- `--baseline true|false` — 是否计算和输出基线统计，默认 `false`
- `--out-baseline <file>` — baseline 输出文件，默认 `baseline.csv`

### 聚类分析

- `--cluster true|false` — 是否进行聚类分析，默认 `false`
- `--cluster-k <K>` — k-means 聚类数（>0 启用 k-means，0 使用阈值聚类），默认 `0`
- `--cluster-threshold <T>` — 相似度阈值（0-1），默认 `0.92`
- `--out-cluster <file>` — 聚类输出文件，默认 `clusters.csv`

### 风险报告

- `--report true|false` — 是否生成风险报告，默认 `false`
- `--out-report <file>` — 报告输出文件，默认 `risk_report.txt`
- `--top-n <N>` — 报告中的 Top N 账户，默认 `20`

### 图表生成

- `--charts true|false` — 是否生成 HTML 图表，默认 `false`
- `--out-chart <file>` — 图表输出路径，默认 `fx_replay_dashboard.html`
- `--chart-top-n <N>` — 图表中的 Top 账户数，默认 `20`

## 输出文件说明

### CSV 文件

| 文件名 | 说明 | 触发条件 |
|--------|------|---------|
| `markout_detail.csv` | 逐笔交易明细，包含 markout、报价年龄、Bot 指标 | 默认输出 |
| `markout_agg_by_account_symbol.csv` | 按账户+品种聚合 | 默认输出 |
| `markout_agg_by_account.csv` | 按账户聚合，含风险评分 | `--agg-account true` |
| `markout_time_buckets.csv` | 时间桶聚合统计 | `--time-bucket-min > 0` |
| `quote_age_stats.csv` | 报价年龄分位数统计 | `--quoteage-stats true` |
| `baseline.csv` | 全局基线统计（均值、标差） | `--baseline true` |
| `clusters.csv` | 聚类结果和相似账户组 | `--cluster true` |
| `bot_indicators.csv` | 账户级 Bot 检测指标 | 自动输出（若有数据） |

### 文本报告

| 文件名 | 说明 | 触发条件 |
|--------|------|---------|
| `risk_report.txt` | 全局基线 + Top N 异常账户详情 | `--report true` |

### HTML 图表

| 文件名 | 说明 | 触发条件 |
|--------|------|---------|
| `fx_replay_dashboard.html` | 交互式 Dashboard（条形图、散点图、表格） | `--charts true` |

## 核心算法和指标

### Markout（市场冲击）

**方向化 markout**：
- **BUY**：`mid_later - mid_exec` （看上涨收益）
- **SELL**：`mid_exec - mid_later` （看下跌收益）

时间窗口：100ms、500ms、1s、5s

**统计**：
- `avg_markout_Xms` — 平均 markout
- `pos_ratio_Xms` — 正收益比例（markout > 0 的笔数占比）

### Quote Age（报价时效性）

定义：当前时刻到最近报价的时间差（毫秒）

**输出**：
- `p50_ms`、`p90_ms`、`p99_ms` — 分位数
- `mean_ms` — 均值

### Z-score（异常检测）

```
Z(X) = (account_mean - global_mean) / global_std
```

用于识别偏离全局基线的异常账户

### 风险评分（Risk Score）

综合 Z-score（markout 500ms、1s 和 quote age），输出 0-100 评分：

```
score = max(0, 50 * (z500 + z1s + zQA) / 3)
```

风险等级：
- **NORMAL**：score < 50
- **LOW**：50 ≤ score < 150
- **MEDIUM**：150 ≤ score < 250
- **HIGH**：score ≥ 250

### Bot 检测

**多维指标**（见 `OfflineAccountTracker`）：
- **系数变异（CV）**：交易大小的标准差 / 均值（低 CV 可能表示机器人）
- **熵值**：TP/SL 价格分布的熵（低熵可能表示固定规则）
- **TP/SL 比例**：有止盈/止损的交易占比
- **客户端指纹**：IP 地址、客户端类型、登录名数量
- **综合 Bot 评分**：0-100，综合上述指标

---

## 高级特性

### 1. 基线统计复用

**优化**：`BaselineStats` 类统一计算全局基线，避免重复计算
- `writeAggAccount()` 返回 `BaselineStats`
- `writeBaseline()` 和 `if (report)` 块共享同一基线数据
- 支持 `--agg-account` 和 `--baseline` 独立或组合使用，基线只计算一次

### 2. CSV 输出统一

**改进**：所有 CSV 输出统一使用 `CsvWriter`，自动处理：
- CSV 特殊字符转义
- 引号处理
- 换行符规范化

所有方法采用：
```java
try (CsvWriter writer = new CsvWriter(path.toString(), ',', StandardCharsets.UTF_8)) {
    writer.writeRecord(headers);
    for (var row : rows) {
        writer.writeRecord(rowValues);
    }
}
```

### 3. 聚合器复用

**流程优化**：
- `ReplayState` 统一管理所有中间结果
- 消除了 `accountAgg` 和 `global` 参数的冗余传递
- 无重复聚合计算

### 4. Markout 计算统一

**方法合并**：
- 删除了重复的 `directionMarkout()` 方法
- 统一使用 `computeMark(Side, double, Double)` 处理方向化 markout 和 null 检查
- 避免了 4 次重复的 markout 计算

---

## 性能优化总结

| 优化项 | 改进 |
|--------|------|
| 消除基线重复计算 | 减少 50% 的 mean/std 调用（当 `--agg-account --baseline` 同时使用时） |
| 消除 markout 重复计算 | 减少 4 次报价查询 + 4 次计算 |
| CSV 输出统一 | 减少代码重复，提高维护性 |
| 参数复用 | 移除 `accountAgg` 和 `global` 冗余参数 |

---

## 示例

### 示例 1：基础分析

```bash
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades examples/trades.csv \
  --quotes examples/quotes.csv
```

输出：`markout_detail.csv`、`markout_agg_by_account_symbol.csv`

### 示例 2：账户聚合 + 基线 + 报告

```bash
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades examples/trades.csv \
  --quotes examples/quotes.csv \
  --agg-account true \
  --baseline true \
  --report true \
  --min-trades 5 \
  --top-n 10
```

输出：完整的聚合数据、基线、风险报告、Bot 指标

### 示例 3：时间桶 + 聚类

```bash
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades examples/trades.csv \
  --quotes examples/quotes.csv \
  --time-bucket-min 5 \
  --bucket-by account \
  --cluster true \
  --cluster-k 5
```

输出：时间桶统计、聚类结果

### 示例 4：完整分析 + 图表

```bash
java -cp target/classes me.asu.ta.FxReplayPlus \
  --trades examples/trades.csv \
  --quotes examples/quotes.csv \
  --agg-account true \
  --baseline true \
  --quoteage-stats true \
  --quoteage-scope all \
  --report true \
  --charts true \
  --cluster true \
  --min-trades 10
```

生成所有分析结果 + HTML Dashboard

---

## 限制和注意事项

1. **数据排序**：交易和报价数据应按时间升序排序
2. **报价覆盖**：每笔交易的历史报价需要覆盖足够长的时间窗口（至少 5 秒）
3. **小样本**：建议使用 `--min-trades` 过滤小样本账户，避免统计偏差
4. **聚类参数**：
   - `--cluster-k 0`：自动选择（基于相似度阈值）
   - `--cluster-k N`：固定 N 个簇（k-means）
5. **Quote Age 采样**：大数据集可用 `--quoteage-max-samples` 限制内存

---

## 故障排查

### Q: 输出文件为空或缺失部分数据

**A**: 检查 `--min-trades` 设置，可能某些账户的交易数不足

### Q: markout 都是 0 或 null

**A**: 检查报价数据覆盖范围，确保每笔交易都有对应的后续报价

### Q: 生成的 baseline.csv 和风险报告中的基线数据不一致

**A**: 现已优化，两者共享同一 `BaselineStats` 对象，不会不一致

### Q: CSV 文件中含有乱码或转义错误

**A**: 现已统一使用 `CsvWriter` 处理，自动正确转义；确保系统 UTF-8 支持