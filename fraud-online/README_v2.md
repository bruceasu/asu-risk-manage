# 交易分析系统 v2.0 - 增强版 README

> **版本**: 2.0 Enhanced  
> **更新日期**: 2026-02-20  
> **新增**: 60+ 机器人检测指标  

---

## 🎯 快速开始

### 系统要求

- **Java**: JDK 17+ (推荐 JDK 21)
- **操作系统**: Windows 10+, Linux, macOS
- **内存**: 最小 2GB，推荐 4GB

### 5分钟快速体验

```bash
# 1. 进入 online 目录
cd online

# 2. 一键编译和运行
run_example.bat    # Windows
# 或
./run_example.sh   # Linux/Mac

# 3. 查看输出
# - 控制台显示实时风控评分
# - classes/ 包含编译的类文件
# - bin/app.jar 可执行 JAR
```

---

## 📦 系统组成

### 1. Online 实时风控系统 (`online/`)

**核心功能：**
- ✅ 实时 Markout 计算（500ms / 1s）
- ✅ Quote Age 监控
- ✅ **[新增]** 时间间隔统计（CV 检测机器人）
- ✅ **[新增]** Request 行为分析（价格偏差、时间差）
- ✅ **[新增]** 客户端指纹追踪（IP、版本、登录名）
- ✅ **[新增]** 订单参数模式检测（大小、止盈止损）
- ✅ **[新增]** 群体同步检测（批量操控）
- ✅ **[新增]** 综合风险评分（6 维度）

**文件结构：**
```
online/
├── src/
│   ├── RealtimeRiskEngineDemo.java    # 主程序
│   ├── RiskLoop.java                  # 风控循环
│   ├── AccountState.java              # 账户状态（已增强）
│   ├── RiskConfig.java                # 风控配置（已增强）
│   │
│   ├── EventText.java                 # [新增] JSON 解析
│   ├── IntervalStats.java             # [新增] 时间间隔统计
│   ├── PriceDeviationTracker.java     # [新增] 价格偏差
│   ├── TimeDiffAnalyzer.java          # [新增] 时间差分析
│   ├── ClientFingerprintAnalyzer.java # [新增] 客户端指纹
│   ├── OrderSizeAnalyzer.java         # [新增] 订单大小
│   ├── TPSLPatternAnalyzer.java       # [新增] 止盈止损模式
│   ├── SyncDetector.java              # [新增] 同步检测
│   ├── EnhancedOrderEvent.java        # [新增] 扩展事件
│   └── IntegrationGuide.java          # [新增] 集成指南
│
├── lib/                               # 可选依赖（目前为空）
├── run_example.bat                    # 构建脚本
└── README.md
```

### 2. Offline 离线分析系统 (`offline/`)

**核心功能：**
- ✅ 历史数据回放
- ✅ Markout 深度分析
- ✅ Quote Age 分位数统计
- ✅ 账户聚类（余弦相似度）
- ✅ 风险报告生成
- ✅ HTML 可视化图表

**文件结构：**
```
offline/
├── src/
│   ├── FxReplayPlus.java              # 主程序
│   ├── FxReplayEngine.java            # 回放引擎
│   ├── FxReplayClusterer.java         # 聚类分析
│   ├── FxReplayWriter.java            # 结果输出
│   └── ...                            # 其他工具类
│
├── examples/
│   ├── trades.csv                     # 示例成交数据
│   └── quotes.csv                     # 示例报价数据
│
└── run_example.bat                    # 构建脚本
```

---

## 🆕 v2.0 新增功能

### 新增指标类别

| 类别 | 指标数 | 核心指标示例 |
|------|--------|-------------|
| **时间维度** | 12 | cv_delta, pct_lt_300ms, orders_last_1s |
| **Request 行为** | 15 | avg_request_price_deviation, negative_time_diff_ratio |
| **订单状态** | 13 | cancel_ratio, avg_order_lifetime_ms |
| **订单参数** | 10 | order_size_entropy, identical_tpsl_ratio |
| **市场环境** | 6 | stale_quote_ratio, avg_spread_bps |
| **跨账户关联** | 8 | sync_strength, max_sync_group_size |

**总计**: 60+ 指标

### 核心检测能力

#### 1. 机器人特征检测
```
✓ CV < 0.15（时间间隔极规律）
✓ 固定止盈止损模式
✓ 固定订单大小（熵 < 1.0）
✓ 超快订单（<300ms）占比 > 50%
```

#### 2. 延迟套利检测
```
✓ Quote Age > 200ms 且 Markout 显著为正
✓ 请求价格偏差持续异常
✓ 时间差异常（负时间差比例高）
```

#### 3. 批量操控检测
```
✓ 多账户同时间窗口（500ms）同向交易
✓ 账户间高度相似（余弦相似度 > 0.9）
✓ 相同 IP 段大量账户
```

#### 4. 账户共享检测
```
✓ 单账户多个登录名
✓ 频繁切换客户端类型/版本
✓ IP 切换频繁
```

---

## 📊 使用示例

### 示例 1：运行 Online 实时风控

```bash
cd online
run_example.bat
```

**输出示例：**
```
[1/4] Compiling Java sources to classes\ ...
[OK] Compilation successful. (18 classes including 8 new trackers)

[2/4] Packaging JAR to bin\app.jar ...
[OK] JAR packaged successfully.

[3/4] Generating 10k test data ...
[OK] Test data generated: examples\events_10000.csv

Done. Build artifacts:
- classes\*.class
- bin\app.jar
```

### 示例 2：查看指标说明

打开 `交易分析指标说明文档.md`，包含：
- 所有 60+ 指标的详细说明
- 计算公式
- 阈值建议
- 使用示例
- SQL 查询示例

### 示例 3：集成到生产环境

查看 `IntegrationGuide.java` 获取完整步骤：
1. Binlog CDC 接入
2. 修改 RiskLoop 集成新指标
3. 数据库持久化
4. 离线分析整合

---

## 🔧 配置说明

### RiskConfig.java 关键参数

```java
// 评分权重
W_Z500 = 0.4;      // Markout 500ms 权重
W_Z1S = 0.2;       // Markout 1s 权重
W_ZQA = 0.1;       // Quote Age 权重
W_CV = 0.15;       // [新增] 时间间隔 CV 权重
W_TPSL = 0.1;      // [新增] 止盈止损模式权重
W_CLIENT = 0.05;   // [新增] 客户端指纹权重

// 风险等级阈值
TH_L1 = 2.0;       // L1 轻度风险
TH_L2 = 3.0;       // L2 中度风险
TH_L3 = 5.0;       // L3 高度风险

// 同步检测参数
SYNC_BUCKET_MS = 500;      // 同步时间桶（毫秒）
SYNC_MIN_ACCOUNTS = 3;     // 最少账户数才算同步
```

### 可调整参数

| 参数 | 默认值 | 说明 | 建议范围 |
|------|--------|------|---------|
| `cv_delta` 阈值 | 0.15 | 机器人检测核心指标 | 0.10-0.20 |
| `pct_lt_300ms` 阈值 | 0.5 | 超快订单占比 | 0.4-0.7 |
| `identical_tpsl_ratio` 阈值 | 0.8 | 固定止盈止损占比 | 0.7-0.9 |
| `order_size_entropy` 阈值 | 1.0 | 订单大小熵 | 0.8-1.2 |

---

## 📖 文档说明

### 核心文档

| 文档名称 | 用途 | 位置 |
|---------|------|------|
| **交易分析指标说明文档.md** | 60+ 指标详细说明 | 根目录 |
| **Maven依赖下载指南.md** | 可选依赖下载 | 根目录 |
| **IntegrationGuide.java** | 生产集成步骤 | online/src/ |
| **EnhancedOrderEvent.java** | 扩展事件示例 | online/src/ |

### 代码注释

所有新增类都包含详细的 JavaDoc 注释：
- 类说明
- 方法用途
- 参数含义
- 可疑阈值
- 使用示例

---

## 🧪 测试与验证

### 单元测试（建议）

创建 `test/` 目录，添加测试类：

```java
// TestIntervalStats.java
public class TestIntervalStats {
    @Test
    public void testBotDetection() {
        long[] deltas = {100, 105, 98, 102, 99};  // 极规律
        IntervalStats stats = IntervalStats.compute(deltas);
        
        assertTrue(stats.cv() < 0.15);
        assertTrue(stats.isBotLike());
    }
}
```

### 集成测试

1. 准备测试数据（已知机器人账户）
2. 运行系统
3. 验证检测准确率

### 性能测试

```bash
# 模拟 10000 笔/秒
java -Xmx2G -jar bin/app.jar --mode synthetic --events-per-sec 10000
```

**预期性能：**
- CPU: < 50%（单核）
- 内存: < 2GB
- 延迟 P99: < 10ms

---

## 🐛 故障排查

### 编译失败

```
[ERROR] Compile failed.
```

**解决方案：**
1. 检查 Java 版本：`java -version`（需要 17+）
2. 检查文件编码：确保 UTF-8
3. 查看具体错误信息

### 运行时错误

```
Exception in thread "main" java.lang.NoClassDefFoundError
```

**解决方案：**
1. 检查 classpath 设置
2. 确认所有 .class 文件已生成
3. 如果使用依赖，确保 lib/*.jar 存在

### 性能问题

```
CPU 100%, 内存持续增长
```

**解决方案：**
1. 减少追踪账户数（`MAX_ACCOUNTS`）
2. 缩短滚动窗口（`ROLL_WIN_SEC`）
3. 定期清理不活跃账户

---

## 📊 输出示例

### 实时风控输出

```
================================================================================
Realtime Risk Engine - Enhanced v2.0
================================================================================

[Global Stats]
- Accounts tracked: 1523
- Total orders: 45678
- Events/sec: 3245

[Top-10 Risk Accounts]
Rank  Account  RiskLvl  Score  CV    MarkOut500  SyncEvents  BotScore
----  -------  -------  -----  ----  ----------  ----------  --------
1     12345    L3       6.52   0.08  +0.00012    23          85
2     23456    L2       4.31   0.14  +0.00008    15          72
3     34567    L2       3.87   0.19  +0.00005    8           58
...

[Enhanced Indicators]
Account 12345:
  - Interval CV: 0.08 (VERY SUSPICIOUS)
  - Pct < 300ms: 0.78 (HIGH)
  - Identical TPSL: 0.92 (FIXED STRATEGY)
  - Order Size Entropy: 0.85 (LOW DIVERSITY)
  - Sync Strength: 0.45 (MODERATE)
  - Bot Likelihood: 85/100 (HIGH)
```

### 离线分析输出

生成的 CSV 文件可以导入 Excel/Python 进行进一步分析：

```csv
# markout_agg_by_account.csv（示例）
account_id,trades,avg_mark_500ms,pos_ratio_500ms,cv_delta,identical_tpsl_ratio,risk_level
12345,523,0.00012,0.78,0.08,0.92,HIGH
23456,412,0.00008,0.65,0.14,0.75,MEDIUM
...
```

---

## 🚀 下一步扩展

### Phase 1（当前版本）✅
- ✅ 60+ 核心指标
- ✅ 实时评分模型
- ✅ 基础追踪器

### Phase 2（规划中）
- ⏳ 账户关系网络图分析
- ⏳ 机器学习模型（XGBoost）
- ⏳ WebSocket 推送实时告警
- ⏳ Grafana 集成可视化

### Phase 3（未来）
- ⏳ 分布式处理（Kafka Streams）
- ⏳ GPU 加速（相似度计算）
- ⏳ 自适应阈值（动态调整）

---

## 🤝 贡献指南

如有改进建议或发现 Bug：

1. 详细描述问题
2. 提供重现步骤
3. 附上日志/错误信息
4. 建议的解决方案（可选）

---

## 📝 版本历史

### v2.0 (2026-02-20) - 增强版
- ✨ 新增 60+ 机器人检测指标
- ✨ 8 个新追踪器类
- ✨ 综合风险评分模型（6 维度）
- ✨ 群体同步检测
- ✨ 完整中文文档

### v1.0 (2026-01-15) - 初始版
- ✅ Markout 分析
- ✅ Quote Age 监控
- ✅ 基础聚类

---

## ⚖️ 许可证

本项目用于学习和研究目的。生产使用请遵守相关金融监管规定。

---

## 📧 联系方式

如需技术支持或咨询，请查阅：
- 📖 `交易分析指标说明文档.md`（最全面）
- 💻 `IntegrationGuide.java`（集成指南）
- 📦 `Maven依赖下载指南.md`（依赖管理）

---

**祝使用愉快！Happy Risk Management! 🎯**
