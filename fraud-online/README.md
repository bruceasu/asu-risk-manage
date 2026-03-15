# Online Realtime Risk Engine Demo

`online/src/RealtimeRiskEngineDemo.java` 是实时风控演示入口。  
本目录已从单文件重构为按功能拆分的多文件结构，便于维护与扩展。

## 1. 目录结构

- 入口与 CLI：
  - `online/src/RealtimeRiskEngineDemo.java`
- 配置：
  - `online/src/RiskConfig.java`
- 事件模型：
  - `online/src/Event.java`
  - `online/src/QuoteEv.java`
  - `online/src/TradeEv.java`
- 基础设施：
  - `online/src/SpscRing.java`
  - `online/src/RollingStats.java`
  - `online/src/Pending.java`
  - `online/src/PendingList.java`
  - `online/src/TimingWheel.java`
- 状态与策略：
  - `online/src/SymbolState.java`
  - `online/src/AccountState.java`
  - `online/src/ExecutionPolicy.java`
- 核心循环：
  - `online/src/RiskLoop.java`
- 数据文件 I/O 与测试数据生成：
  - `online/src/EventCsvIO.java`
  - `online/src/DataGen10k.java`

## 2. 功能概览

- SPSC ring buffer 事件摄取
- 时间轮延迟结算 markout（500ms / 1s）
- 15 分钟滚动窗口统计（1 秒桶）
- 全局基线与账户 z-score
- 迟滞风险等级（L0-L3）
- 简化限速策略
- Top-N 异常账户周期输出
- 支持文件回放模式（用于离线验证）

## 3. 编译与打包

```bash
javac -d online/classes online/src/*.java
jar --create --file online/bin/app.jar --main-class RealtimeRiskEngineDemo -C online/classes .
```

## 4. 帮助参数

```bash
java -jar online/bin/app.jar --help
java -jar online/bin/app.jar -h
```

## 5. 运行方式

### 5.1 Synthetic 模式（默认）

```bash
java -jar online/bin/app.jar --mode synthetic --events 20000 --active-accounts 200
```

### 5.2 File 模式（回放 CSV）

```bash
java -jar online/bin/app.jar --mode file --events-file online/examples/events_10000.csv
```

输出风险账户快照（CSV）：

```bash
java -jar online/bin/app.jar \
  --mode file \
  --events-file online/examples/events_10000.csv \
  --risk-out online/risk_accounts.csv \
  --risk-min-n 30
```

## 6. 1 万行测试数据验证

生成数据（事件 CSV）：

```bash
java -cp online/classes DataGen10k
```

输出文件：

- `online/examples/events_10000.csv`

说明：

- 列格式：`type,ts,symbol_id,mid,account_id,side`
- `type=Q` 表示报价事件，`type=T` 表示成交事件
- 该文件用于 `--mode file` 进行回放验证

## 7. 一键脚本（与 offline 同风格）

```bash
online/run_example.bat
```

脚本会完成：

1. 编译到 `online/classes`
2. 打包 `online/bin/app.jar`
3. 生成 `online/examples/events_10000.csv`
4. 使用 file 模式回放验证
5. 生成风险账户快照 `online/risk_accounts.csv`

## 8. 风险快照字段说明

`risk_accounts.csv` 字段：

- `ts_ms`：快照时间（毫秒）
- `account_id`：账户 ID
- `level`：风险等级（0-3）
- `score`：综合分数
- `z500`：500ms markout z-score
- `z1s`：1s markout z-score
- `zqa`：quote_age z-score
- `extra_delay_ms`：策略附加延迟
- `last_look`：是否启用 last look
- `max_ops_per_second`：每秒最大操作数
- `n500`：500ms 样本量
