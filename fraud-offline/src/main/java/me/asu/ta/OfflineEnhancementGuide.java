package me.asu.ta;

/**
 * <h1>Offline系统增强指南</h1>
 * 
 * <h2>目标</h2>
 * 将online系统的60+bot检测指标集成到offline批量回放分析中，实现：
 * <ul>
 *   <li>1. 历史数据深度分析：识别长期bot行为模式</li>
 *   <li>2. 模型优化：基于历史数据调优检测阈值</li>
 *   <li>3. 关联分析：发现"高bot分数 + 高markout"账户</li>
 *   <li>4. 可视化报告：dashboard同时展示markout和bot指标</li>
 * </ul>
 * 
 * <h2>修改路线图</h2>
 * 
 * <h3>P0: 数据格式扩展（用户任务，~20分钟）</h3>
 * 
 * <h4>步骤1：扩展trades.csv格式</h4>
 * <pre>
 * 原格式：
 * account_id,symbol,side,exec_time_ms,size
 * A001,EURUSD,BUY,1700000000500,100000
 * 
 * 新格式（添加4列）：
 * account_id,symbol,side,exec_time_ms,size,eventText,orderSize,takeProfit,stopLoss
 * A001,EURUSD,BUY,1700000000500,100000,"{\"requestPrice\":1.0850,\"clientIP\":\"192.168.1.100\",\"loginName\":\"trader01\",\"clientType\":\"MT4\"}",100000,1.0860,1.0840
 * A002,USDJPY,SELL,1700000001200,50000,"{\"requestPrice\":149.50,\"clientIP\":\"192.168.1.101\",\"loginName\":\"trader02\",\"clientType\":\"cTrader\"}",50000,149.30,149.60
 * </pre>
 * 
 * <p><b>注意</b>：eventText字段是JSON字符串，需要转义双引号。如果从MySQL导出，可使用：</p>
 * <pre>
 * SELECT 
 *   account_id,
 *   symbol,
 *   side,
 *   exec_time_ms,
 *   size,
 *   REPLACE(EventText, '"', '\"') as eventText,  -- 转义JSON
 *   order_size as orderSize,
 *   take_profit as takeProfit,
 *   stop_loss as stopLoss
 * FROM OrderExtPo
 * WHERE exec_time_ms BETWEEN ? AND ?
 * ORDER BY exec_time_ms
 * INTO OUTFILE '/path/to/trades_enhanced.csv'
 * FIELDS TERMINATED BY ',' ENCLOSED BY '' ESCAPED BY '\\'
 * LINES TERMINATED BY '\n';
 * </pre>
 * 
 * <h3>P1: 核心引擎修改（编码任务，~2-3小时）</h3>
 * 
 * <h4>步骤2：复制online追踪器类</h4>
 * <p>将以下文件从 online/src/ 复制到 offline/src/：</p>
 * <ul>
 *   <li>EventText.java - JSON解析器</li>
 *   <li>IntervalStats.java - 时间间隔统计结构（包含compute方法）</li>
 *   <li>PriceDeviationTracker.java - Request价格偏离追踪</li>
 *   <li>TimeDiffAnalyzer.java - 时间间隔分析</li>
 *   <li>ClientFingerprintAnalyzer.java - 客户端指纹</li>
 *   <li>OrderSizeAnalyzer.java - 订单大小熵计算</li>
 *   <li>TPSLPatternAnalyzer.java - TP/SL模式分析</li>
 *   <li>SyncDetector.java - 同步行为检测（可选）</li>
 * </ul>
 * 
 * <p>PowerShell命令：</p>
 * <pre>
 * cd C:\Users\svictor\workspace\tools\交易分析
 * Copy-Item online\src\EventText.java offline\src\
 * Copy-Item online\src\IntervalStats.java offline\src\
 * Copy-Item online\src\*Tracker.java offline\src\
 * Copy-Item online\src\*Analyzer.java offline\src\
 * Copy-Item online\src\SyncDetector.java offline\src\
 * </pre>
 * 
 * <h4>步骤3：创建OfflineAccountTracker</h4>
 * <p>在offline/src/中创建新文件 OfflineAccountTracker.java：</p>
 * <pre>
 * import java.util.*;
 * 
 * // 离线回放时为每个账户维护的追踪器集合
 * final class OfflineAccountTracker {
 *     private final List&lt;Long&gt; orderTimes = new ArrayList&lt;&gt;();
 *     private final PriceDeviationTracker priceDeviation = new PriceDeviationTracker();
 *     private final TimeDiffAnalyzer timeDiffAnalyzer = new TimeDiffAnalyzer();
 *     private final ClientFingerprintAnalyzer clientFingerprint = new ClientFingerprintAnalyzer();
 *     private final OrderSizeAnalyzer sizeAnalyzer = new OrderSizeAnalyzer();
 *     private final TPSLPatternAnalyzer tpslPattern = new TPSLPatternAnalyzer();
 *     
 *     void addOrderTime(long ts) {
 *         orderTimes.add(ts);
 *     }
 *     
 *     void addEventText(EventText et, double execPrice, String side) {
 *         if (et != null) {
 *             priceDeviation.add(et.requestPrice(), execPrice, side);
 *             clientFingerprint.add(et);
 *         }
 *     }
 *     
 *     void addOrderSize(double size) {
 *         sizeAnalyzer.add(size);
 *     }
 *     
 *     void addTPSL(Double tp, Double sl, double execPrice, String side) {
 *         tpslPattern.add(tp, sl, execPrice, side);
 *     }
 *     
 *     // 计算最终统计指标
 *     IntervalStats computeStats() {
 *         return IntervalStats.compute(orderTimes, timeDiffAnalyzer);
 *     }
 *     
 *     double getEntropy() {
 *         return sizeAnalyzer.entropy();
 *     }
 *     
 *     double getTPSLRatio() {
 *         return tpslPattern.usageRatio();
 *     }
 *     
 *     int getClientIPCount() {
 *         return clientFingerprint.ipCount();
 *     }
 *     
 *     String getClientTypes() {
 *         return String.join("|", clientFingerprint.clientTypeSet());
 *     }
 * }
 * </pre>
 * 
 * <h4>步骤4：修改FxReplayEngine.java</h4>
 * <p>在 replay() 方法中添加追踪器管理：</p>
 * 
 * <pre>
 * // FxReplayEngine.java - 在replay()方法开始处添加
 * static ReplayState replay(...) throws IOException {
 *     ReplayState st = new ReplayState();
 *     
 *     // 新增：为每个账户维护追踪器
 *     Map&lt;String, OfflineAccountTracker&gt; accountTrackers = new HashMap&lt;&gt;();
 *     
 *     try (BufferedReader br = Files.newBufferedReader(tradesCsv, StandardCharsets.UTF_8)) {
 *         String headerLine = br.readLine();
 *         if (headerLine == null) throw new IllegalArgumentException("Empty trades file");
 *         String[] header = FxReplayCommon.splitCsvLine(headerLine);
 *         Csv csv = new Csv(header);
 *         
 *         // 原有列索引
 *         int iAcc = csv.col(COL_ACC);
 *         int iSym = csv.col(COL_SYM);
 *         int iSide = csv.col(COL_SIDE);
 *         int iT = csv.col(COL_EXEC_T);
 *         Integer iSizeOpt = csv.colOptional(COL_SIZE);
 *         int iSize = iSizeOpt != null ? iSizeOpt : -1;
 *         
 *         // 新增列索引（可选）
 *         Integer iEventTextOpt = csv.colOptional("eventText");
 *         int iEventText = iEventTextOpt != null ? iEventTextOpt : -1;
 *         Integer iOrderSizeOpt = csv.colOptional("orderSize");
 *         int iOrderSize = iOrderSizeOpt != null ? iOrderSizeOpt : -1;
 *         Integer iTPOpt = csv.colOptional("takeProfit");
 *         int iTP = iTPOpt != null ? iTPOpt : -1;
 *         Integer iSLOpt = csv.colOptional("stopLoss");
 *         int iSL = iSLOpt != null ? iSLOpt : -1;
 *         
 *         // 在读取每行trade的循环中：
 *         while ((line = br.readLine()) != null) {
 *             // ... 原有解析代码 ...
 *             
 *             // 新增：解析增强字段
 *             String eventTextStr = (iEventText >= 0 && r.length > iEventText) ? r[iEventText].trim() : null;
 *             Double orderSize = (iOrderSize >= 0 && r.length > iOrderSize) 
 *                 ? FxReplayCommon.parseDoubleSafe(r[iOrderSize]) : null;
 *             Double takeProfit = (iTP >= 0 && r.length > iTP) 
 *                 ? FxReplayCommon.parseDoubleSafe(r[iTP]) : null;
 *             Double stopLoss = (iSL >= 0 && r.length > iSL) 
 *                 ? FxReplayCommon.parseDoubleSafe(r[iSL]) : null;
 *             
 *             // 获取或创建追踪器
 *             OfflineAccountTracker tracker = accountTrackers.computeIfAbsent(
 *                 acc, k -> new OfflineAccountTracker()
 *             );
 *             
 *             // 添加订单时间（必须）
 *             tracker.addOrderTime(t0);
 *             
 *             // 添加EventText相关数据（如果存在）
 *             if (eventTextStr != null && !eventTextStr.isEmpty()) {
 *                 EventText et = EventText.parse(eventTextStr);
 *                 tracker.addEventText(et, mid0, side.toString());
 *             }
 *             
 *             // 添加订单大小（如果存在）
 *             if (orderSize != null) {
 *                 tracker.addOrderSize(orderSize);
 *             }
 *             
 *             // 添加TP/SL（如果存在）
 *             if (takeProfit != null || stopLoss != null) {
 *                 tracker.addTPSL(takeProfit, stopLoss, mid0, side.toString());
 *             }
 *             
 *             // ... 原有markout计算代码 ...
 *         }
 *     }
 *     
 *     // 在返回前，将accountTrackers保存到ReplayState
 *     st.accountTrackers = accountTrackers;
 *     return st;
 * }
 * </pre>
 * 
 * <h4>步骤5：修改ReplayState.java</h4>
 * <p>添加字段存储追踪器：</p>
 * <pre>
 * final class ReplayState {
 *     long usedTrades = 0;
 *     long skippedTrades = 0;
 *     final List&lt;DetailRow&gt; details = new ArrayList&lt;&gt;();
 *     final Map&lt;String, LongSamples&gt; quoteAgeByKey = new HashMap&lt;&gt;();
 *     
 *     // 新增：存储每个账户的追踪器
 *     Map&lt;String, OfflineAccountTracker&gt; accountTrackers = new HashMap&lt;&gt;();
 * }
 * </pre>
 * 
 * <h4>步骤6：修改DetailRow.java</h4>
 * <p>添加bot检测指标字段：</p>
 * <pre>
 * class DetailRow {
 *     // 原有字段
 *     final String account, symbol, side;
 *     final long execTimeMs;
 *     final double size;
 *     final double mid0;
 *     final long lastQuoteT0;
 *     final long quoteAgeMs;
 *     final Double[] mids = new Double[DELTAS_MS.length];
 *     final Double[] marks = new Double[DELTAS_MS.length];
 *     
 *     // 新增：bot检测指标（账户级别，从OfflineAccountTracker计算）
 *     Double cv;              // 时间间隔变异系数
 *     Integer botScore;       // bot综合评分 0-100
 *     Double entropy;         // 订单大小熵
 *     Double tpslRatio;       // TP/SL使用比例
 *     Integer clientIPCount;  // 客户端IP数量
 *     String clientTypes;     // 客户端类型列表
 *     
 *     // 构造函数保持不变...
 *     
 *     // 新增：设置bot指标的方法
 *     void setBotIndicators(IntervalStats stats, double entropy, double tpslRatio, 
 *                          int ipCount, String types) {
 *         if (stats != null) {
 *             this.cv = stats.cv();
 *             this.botScore = stats.isBotLike() ? 
 *                 Math.min(100, (int)(stats.cv() < 0.10 ? 95 : 
 *                                    stats.cv() < 0.15 ? 80 : 60)) : 0;
 *         }
 *         this.entropy = entropy;
 *         this.tpslRatio = tpslRatio;
 *         this.clientIPCount = ipCount;
 *         this.clientTypes = types;
 *     }
 * }
 * </pre>
 * 
 * <h4>步骤7：修改FxReplayWriter.java</h4>
 * <p>在输出CSV时添加bot指标列：</p>
 * <pre>
 * // 修改 writeDetail() 方法
 * static void writeDetail(List&lt;DetailRow&gt; rows, Path out) throws IOException {
 *     try (var w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
 *         // 原有列头
 *         w.write("account,symbol,side,exec_time_ms,size,mid0,last_quote_t0,quote_age_ms");
 *         for (String dn : DetailRow.DELTA_NAMES) {
 *             w.write(",mid_");
 *             w.write(dn);
 *             w.write(",mark_");
 *             w.write(dn);
 *         }
 *         // 新增：bot指标列头
 *         w.write(",cv,botScore,entropy,tpslRatio,clientIPs,clientTypes");
 *         w.write("\n");
 *         
 *         for (DetailRow r : rows) {
 *             // 原有列输出...
 *             w.write(r.account);
 *             w.write(',');
 *             w.write(r.symbol);
 *             // ... 省略其他列 ...
 *             
 *             // 新增：bot指标列输出
 *             w.write(',');
 *             w.write(r.cv != null ? String.format("%.4f", r.cv) : "");
 *             w.write(',');
 *             w.write(r.botScore != null ? r.botScore.toString() : "");
 *             w.write(',');
 *             w.write(r.entropy != null ? String.format("%.4f", r.entropy) : "");
 *             w.write(',');
 *             w.write(r.tpslRatio != null ? String.format("%.4f", r.tpslRatio) : "");
 *             w.write(',');
 *             w.write(r.clientIPCount != null ? r.clientIPCount.toString() : "");
 *             w.write(',');
 *             w.write(r.clientTypes != null ? r.clientTypes : "");
 *             w.write('\n');
 *         }
 *     }
 * }
 * 
 * // 修改聚合输出：在writeAggByAccount()中添加bot指标的平均值
 * static void writeAggByAccount(...) {
 *     // 在输出时计算每个账户的平均cv、botScore等
 *     // 从accountTrackers中提取数据
 * }
 * </pre>
 * 
 * <h4>步骤8：修改FxReplayPlus.java主程序</h4>
 * <p>在生成DetailRow时设置bot指标：</p>
 * <pre>
 * // 在FxReplayPlus.main()或run()中，处理完replay后：
 * ReplayState st = FxReplayEngine.replay(...);
 * 
 * // 为每个DetailRow设置bot指标
 * for (DetailRow dr : st.details) {
 *     OfflineAccountTracker tracker = st.accountTrackers.get(dr.account);
 *     if (tracker != null) {
 *         IntervalStats stats = tracker.computeStats();
 *         dr.setBotIndicators(
 *             stats,
 *             tracker.getEntropy(),
 *             tracker.getTPSLRatio(),
 *             tracker.getClientIPCount(),
 *             tracker.getClientTypes()
 *         );
 *     }
 * }
 * 
 * // 然后输出
 * FxReplayWriter.writeDetail(st.details, outDetail);
 * </pre>
 * 
 * <h3>P2: 专项输出（可选，~1小时）</h3>
 * 
 * <h4>步骤9：创建bot_indicators.csv专项输出</h4>
 * <p>新增方法 FxReplayWriter.writeBotIndicators()：</p>
 * <pre>
 * static void writeBotIndicators(Map&lt;String, OfflineAccountTracker&gt; trackers, 
 *                                Path out) throws IOException {
 *     try (var w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
 *         w.write("account_id,cv,botScore,isBotLike,entropy,");
 *         w.write("tpslRatio,clientIPs,clientTypes,totalOrders\n");
 *         
 *         for (var entry : trackers.entrySet()) {
 *             String acc = entry.getKey();
 *             OfflineAccountTracker t = entry.getValue();
 *             IntervalStats stats = t.computeStats();
 *             
 *             w.write(acc);
 *             w.write(',');
 *             w.write(stats != null ? String.format("%.4f", stats.cv()) : "");
 *             w.write(',');
 *             w.write(stats != null && stats.isBotLike() ? "1" : "0");
 *             w.write(',');
 *             w.write(String.format("%.4f", t.getEntropy()));
 *             w.write(',');
 *             w.write(String.format("%.4f", t.getTPSLRatio()));
 *             w.write(',');
 *             w.write(String.valueOf(t.getClientIPCount()));
 *             w.write(',');
 *             w.write(t.getClientTypes());
 *             w.write(',');
 *             w.write(stats != null ? String.valueOf(stats.orderCount()) : "0");
 *             w.write('\n');
 *         }
 *     }
 * }
 * </pre>
 * 
 * <h3>P3: 可视化增强（按需，~1-2小时）</h3>
 * 
 * <h4>步骤10：修改fx_replay_dashboard.html</h4>
 * <p>在FxReplayCharts.java生成的HTML中添加：</p>
 * <ul>
 *   <li>CV分布直方图（x轴：CV值区间，y轴：账户数）</li>
 *   <li>Bot分数分布图（x轴：0-100分段，y轴：账户数）</li>
 *   <li>Entropy vs Markout散点图（识别高风险区域）</li>
 *   <li>高风险账户表格（bot>80 且 markout_500ms>0）</li>
 * </ul>
 * 
 * <h2>测试验证</h2>
 * 
 * <h3>测试数据准备</h3>
 * <pre>
 * 1. 使用online/DataExtensionGuide.java中的测试数据生成器
 * 2. 生成1000笔测试数据，包含3种账户类型：
 *    - 正常账户：CV=0.5，随机订单大小，无固定TP/SL
 *    - bot账户：CV=0.08，固定订单大小，总是设置TP/SL
 *    - 混合账户：CV=0.18，中等变异
 * 3. 保存为 offline/examples/trades_enhanced.csv
 * </pre>
 * 
 * <h3>编译运行</h3>
 * <pre>
 * cd offline
 * javac -d classes src/*.java
 * java -cp classes FxReplayPlus ^
 *   --trades examples/trades_enhanced.csv ^
 *   --quotes examples/quotes.csv ^
 *   --out-detail markout_detail_enhanced.csv ^
 *   --out-bot-indicators bot_indicators.csv
 * </pre>
 * 
 * <h3>预期输出</h3>
 * <p>markout_detail_enhanced.csv应该包含：</p>
 * <pre>
 * account,symbol,...,cv,botScore,entropy,tpslRatio,clientIPs,clientTypes
 * bot_001,EURUSD,...,0.0823,92,0.12,0.98,1,MT4
 * normal_001,EURUSD,...,0.5234,0,2.45,0.15,3,MT4|cTrader|WebTrader
 * </pre>
 * 
 * <p>bot_indicators.csv应该突出显示bot账户：</p>
 * <pre>
 * account_id,cv,botScore,isBotLike,entropy,tpslRatio,clientIPs,clientTypes,totalOrders
 * bot_001,0.0823,92,1,0.12,0.98,1,MT4,500
 * normal_001,0.5234,0,0,2.45,0.15,3,MT4|cTrader,450
 * </pre>
 * 
 * <h2>与online系统对比验证</h2>
 * 
 * <pre>
 * 1. 使用相同的测试数据分别跑online和offline
 * 2. 对比同一账户的指标是否一致：
 *    - CV差异应 &lt; 0.01
 *    - Entropy差异应 &lt; 0.05
 *    - botScore差异应 &lt; 5分
 * 3. 如果差异过大，检查：
 *    - 时间窗口是否一致（online用滚动窗口，offline用全量）
 *    - 数据顺序是否一致
 *    - 计算逻辑是否完全相同
 * </pre>
 * 
 * <h2>后续优化建议</h2>
 * 
 * <ol>
 *   <li><b>性能优化</b>：如果处理百万级别数据，考虑：
 *     <ul>
 *       <li>使用多线程并行处理不同账户</li>
 *       <li>定期清理不活跃账户的追踪器</li>
 *       <li>使用primitive collections减少装箱开销</li>
 *     </ul>
 *   </li>
 *   <li><b>功能增强</b>：
 *     <ul>
 *       <li>添加时间分段统计（按小时/天分组）</li>
 *       <li>实现账户聚类（基于bot特征相似度）</li>
 *       <li>生成风控报告PDF（集成markout + bot指标）</li>
 *     </ul>
 *   </li>
 *   <li><b>数据管道</b>：
 *     <ul>
 *       <li>创建自动化脚本：MySQL导出 → offline分析 → 报告生成</li>
 *       <li>集成到定时任务（每日生成昨日的bot分析报告）</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <h2>FAQ</h2>
 * 
 * <h3>Q1: 如果trades.csv没有eventText列会怎样？</h3>
 * <p>A: 系统会优雅降级，只输出基础的CV和时间间隔统计。entropy、tpslRatio等字段为空。</p>
 * 
 * <h3>Q2: offline的CV值和online不一致？</h3>
 * <p>A: 正常现象。online用15分钟滚动窗口，offline用全量数据。如果账户行为模式在不同时段变化较大，会有差异。</p>
 * 
 * <h3>Q3: 如何快速定位高风险账户？</h3>
 * <p>A: 在bot_indicators.csv中筛选：cv&lt;0.15 AND entropy&lt;0.5 AND tpslRatio&gt;0.8</p>
 * 
 * <h3>Q4: 可以跟SyncDetector集成吗？</h3>
 * <p>A: 可以！在OfflineAccountTracker中添加SyncDetector实例，在回放时调用detectSync()。但注意需要全局视角（跨账户），建议在FxReplayEngine层面维护。</p>
 * 
 * <h2>联系与支持</h2>
 * <p>如有问题，参考以下文档：</p>
 * <ul>
 *   <li>online/src/DataExtensionGuide.java - online系统数据扩展指南</li>
 *   <li>交易分析指标说明文档.md - 60+指标详细说明</li>
 *   <li>任务完成总结.md - online系统实施总结</li>
 * </ul>
 */
public class OfflineEnhancementGuide {
    // 此文件仅作为文档，不包含可执行代码
    private OfflineEnhancementGuide() {}
}