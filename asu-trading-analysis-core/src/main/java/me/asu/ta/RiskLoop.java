package me.asu.ta;

import me.asu.ta.dto.Event;
import me.asu.ta.dto.EventText;
import me.asu.ta.dto.OrderEvent;
import me.asu.ta.dto.QuoteEvent;
import me.asu.ta.util.SpscRing;import me.asu.ta.util.SymbolMapper;
import me.asu.ta.util.TimingWheel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


public final class RiskLoop {
    private static final ExecutionPolicy[] POLICIES = new ExecutionPolicy[] {
            new ExecutionPolicy(0, false, RiskConfig.L0_MAX_OPS),
            new ExecutionPolicy(50, false, RiskConfig.L1_MAX_OPS),
            new ExecutionPolicy(120, true, RiskConfig.L2_MAX_OPS),
            new ExecutionPolicy(250, true, RiskConfig.L3_MAX_OPS)
    };

    private final SpscRing<Event> ring;
    private final SymbolState[] symbols;
    private final AccountState[] accounts;
    private final TimingWheel wheel;

    private final RollingStats gMark500 = new RollingStats();
    private final RollingStats gMark1s = new RollingStats();
    private final RollingStats gQAge = new RollingStats();

    private final SymbolMapper symbolMapper = new SymbolMapper(); // 用于 symbolId -> symbolName 映射
    private final SyncDetector syncDetector = new SyncDetector(RiskConfig.SYNC_BUCKET_MS);

    private long nextPrintMs = Long.MIN_VALUE;
    private final SnapshotFileWriter snapshotWriter;
    private final int riskMinN;

    public RiskLoop(SpscRing<Event> ring, int symbolCount, int accountCount) {
        RiskEngineConfig config = new RiskEngineConfig();
        config.ring = ring;
        config.symbolCount = symbolCount;
        config.accountCount = accountCount;
        config.snapshotWriter = null;
        config.riskMinN = 30;

        this(config);
    }

    public RiskLoop(RiskEngineConfig config) {
        this.ring = config.ring;
        this.symbols = new SymbolState[config.symbolCount];
        for (int i = 0; i < config.symbolCount; i++) symbols[i] = new SymbolState();
        
        this.accounts = new AccountState[config.accountCount];

        for (int i = 0; i < config.accountCount; i++) accounts[i] = new AccountState();
        
        this.wheel = new TimingWheel();
        this.snapshotWriter = config.snapshotWriter;
        this.riskMinN = Math.max(1, config.riskMinN);
    }

    public void runOnce(long nowMs) {
        Event e;
        while ((e = ring.poll()) != null) {
            if (e instanceof QuoteEvent q)
                onQuote(q);
            else if (e instanceof OrderEvent t)
                onTrade(t);
        }
        wheel.advanceTo(nowMs, this::settle);

        if (nextPrintMs == Long.MIN_VALUE)
            nextPrintMs = nowMs + 1000;
        if (nowMs >= nextPrintMs) {
            printTopN(nowMs);
            nextPrintMs += 1000;
        }
    }

    public void onQuote(QuoteEvent q) {
        SymbolState s = symbols[q.symbolId()];
        s.lastMid = q.mid();
        s.lastQuoteTs = q.ts();
        s.hasQuote = true;
    }

    public void onTrade(OrderEvent t) {
        AccountState a = accounts[t.accountId];
        SymbolState s = symbols[t.symbolId];
        if (s.hasQuote) {
            // Quote Age 计算
            long qAge = t.transactionTime - s.lastQuoteTs;

            a.qAge.add(qAge, t.transactionTime);
            gQAge.add(qAge, t.transactionTime);

            schedule(t, s.lastMid, 500, (short) 0);
            schedule(t, s.lastMid, 1000, (short) 1);

            // Markout 调度
            // scheduleMarkoutSettlement(o, s, a, qAge);
        }


        // ========== 1. 添加订单时间到循环缓冲（用于时间间隔分析）
        a.addOrderTime(t.orderTime);


        // ========== 2. 解析 event_text（如果CSV或数据库有该字段）
        String eventTextJson = t.eventText; // 需要添加到 TradeEv
        EventText et = EventText.parse(eventTextJson);

        // ========== 3. 价格偏差追踪（需要 requestPrice 字段）
        if (et != null && et.requestPrice != null) {
            a.priceDeviation.add(et.requestPrice, t.price);
        }

        // ========== 4. 时间差分析（需要 clientTimestamp 字段）
        if (et != null && et.timeDiff != null) {
            a.timeDiffAnalyzer.add(et.timeDiff);
        }

        // ========== 5. 客户端指纹追踪（需要完整 EventText）
        if (et != null) {
            a.clientFingerprint.add(et);
        }

        // ========== 6. 订单大小分析
        a.sizeAnalyzer.add(t.orderQty);

        // ========== 7. 止盈止损模式检测（需要 TP/SL 字段）
        BigDecimal tp = t.takeProfitPrice != null ? t.takeProfitPrice : null;
        BigDecimal sl = t.stopLossPrice != null ? t.stopLossPrice : null;
        if (tp != null && sl != null) {
            a.tpslPattern.add(tp, sl, t.price);
        }

        // ========== 8. 跨账户同步检测（全局）
        int symbolId = t.symbolId; // 需要映射到 symbol name
        String symbolName = symbolMapper.getName(symbolId);
        syncDetector.onOrder(t.accountId, symbolName, t.side, t.ts());
    }

    private void schedule(OrderEvent t, double mid0, long deltaMs, short deltaIdx) {
        Pending p = new Pending();
        p.accountId = t.accountId;
        p.symbolId = t.symbolId;
        p.dueTs = t.ts() + deltaMs;
        p.mid0 = mid0;
        p.side = t.side;
        p.deltaIdx = deltaIdx;
        wheel.schedule(p);
    }

    private void settle(Pending p) {
        SymbolState s = symbols[p.symbolId];
        if (!s.hasQuote)
            return;
        double mark = (p.side == 1) ? (s.lastMid - p.mid0) : (p.mid0 - s.lastMid);

        AccountState a = accounts[p.accountId];
        if (p.deltaIdx == 0) {
            a.mark500.add(mark, p.dueTs);
            gMark500.add(mark, p.dueTs);
        } else {
            a.mark1s.add(mark, p.dueTs);
            gMark1s.add(mark, p.dueTs);
        }
        updateRiskLevel(p.accountId);
    }

    private void updateRiskLevel(int accountId) {
        AccountState a = accounts[accountId];
        double z500 = z(a.mark500, gMark500);
        double z1s = z(a.mark1s, gMark1s);
        double zqa = z(a.qAge, gQAge);

        // Enhanced v2.0: 使用增强评分模型（6维度）
        // 如果有足够的订单样本，使用增强评分
        IntervalStats ivl = a.getIntervalStats();
        double score;
        if (ivl != null && ivl != IntervalStats.EMPTY && ivl.mean() > 0) {
            // 使用增强评分（需要全局统计参数）
            score = RiskConfig.computeEnhancedScore(a,
                    gMark500.mean(), gMark500.std(),
                    gMark1s.mean(), gMark1s.std(),
                    gQAge.mean(), gQAge.std());
        } else {
            // 样本不足，使用传统评分
            score = RiskConfig.W_Z500 * z500 + RiskConfig.W_Z1S * z1s + RiskConfig.W_ZQA * zqa;
        }

        byte cur = a.level;
        byte target = scoreToLevel(score);
        if (target > cur) {
            a.upCount++;
            a.downCount = 0;
            if (a.upCount >= RiskConfig.ENTER_K) {
                a.level = target;
                a.upCount = 0;
            }
        } else if (target < cur) {
            a.downCount++;
            a.upCount = 0;
            if (a.downCount >= RiskConfig.EXIT_M) {
                a.level = target;
                a.downCount = 0;
            }
        } else {
            a.upCount = 0;
            a.downCount = 0;
        }
    }

    private static byte scoreToLevel(double score) {
        if (score >= RiskConfig.TH_L3)
            return 3;
        if (score >= RiskConfig.TH_L2)
            return 2;
        if (score >= RiskConfig.TH_L1)
            return 1;
        return 0;
    }

    private static double z(RollingStats a, RollingStats g) {
        double sd = g.std();
        if (sd == 0)
            return 0;
        return (a.mean() - g.mean()) / sd;
    }

    public ExecutionPolicy policyFor(int accountId) {
        return POLICIES[accounts[accountId].level];
    }

    public boolean shouldAllowOp(int accountId, long tsMs) {
        AccountState a = accounts[accountId];
        long sec = tsMs / 1000;
        if (a.rlSec != sec) {
            a.rlSec = sec;
            a.rlCount = 0;
        }
        int limit = policyFor(accountId).maxOpsPerSecond();
        if (a.rlCount >= limit)
            return false;
        a.rlCount++;
        return true;
    }

    public void printTopN(long nowMs) {
        PriorityQueue<AccScore> pq = new PriorityQueue<>(Comparator.comparingDouble(x -> x.score));
        for (int acc = 0; acc < accounts.length; acc++) {
            AccountState a = accounts[acc];
            if (a.mark500.n() < riskMinN)
                continue;

            double z500 = z(a.mark500, gMark500);
            double z1s = z(a.mark1s, gMark1s);
            double zqa = z(a.qAge, gQAge);

            // 计算时间间隔统计和机器人评分
            IntervalStats ivl = a.getIntervalStats();
            double score;
            double botScore = 0.0;
            double cv = 0.0;
            if (ivl != null && ivl != IntervalStats.EMPTY && ivl.mean() > 0) {
                cv = ivl.cv();
                score = RiskConfig.computeEnhancedScore(a,
                        gMark500.mean(), gMark500.std(),
                        gMark1s.mean(), gMark1s.std(),
                        gQAge.mean(), gQAge.std());
                botScore = RiskConfig.computeBotLikelihoodScore(a);
            } else {
                score = RiskConfig.W_Z500 * z500 + RiskConfig.W_Z1S * z1s + RiskConfig.W_ZQA * zqa;
            }

            if (pq.size() < RiskConfig.TOP_N)
                pq.add(new AccScore(acc, score, z500, z1s, zqa, a.level, cv, (int) botScore));
            else if (score > pq.peek().score) {
                pq.poll();
                pq.add(new AccScore(acc, score, z500, z1s, zqa, a.level, cv, (int) botScore));
            }
        }

        List<AccScore> list = new ArrayList<>(pq);
        list.sort((a, b) -> Double.compare(b.score, a.score));

        System.out.println("== " + Instant.ofEpochMilli(nowMs) + " Top-" + RiskConfig.TOP_N
                + " abnormal (score) ==");
        System.out.printf(Locale.ROOT,
                "Global: mean500=%.6g std500=%.6g | meanQA=%.2fms stdQA=%.2fms | n500=%d%n",
                gMark500.mean(), gMark500.std(), gQAge.mean(), gQAge.std(), gMark500.n());
        for (AccScore s : list) {
            ExecutionPolicy p = POLICIES[s.level];
            AccountState a = accounts[s.accountId];

            System.out.printf(Locale.ROOT,
                    "acc=%d lvl=L%d score=%.2f z500=%.2f z1s=%.2f zQA=%.2f | CV=%.3f bot=%d entropy=%.2f tpsl=%.2f | policy(delay=%dms lastLook=%s rps=%d) n500=%d%n",
                    s.accountId, s.level, s.score, s.z500, s.z1s, s.zqa,
                    s.cv, s.botScore, a.sizeAnalyzer.entropy(), a.tpslPattern.identicalTPSLRatio(),
                    p.extraDelayMs(), p.lastLook(), p.maxOpsPerSecond(), a.mark500.n());
            if (snapshotWriter != null) {
                try {
                    snapshotWriter.write(nowMs, s.accountId, s.level, s.score, s.z500, s.z1s, s.zqa,
                            p,
                            accounts[s.accountId].mark500.n());
                } catch (Exception ignored) {
                    // keep loop running even if disk write fails
                }
            }
        }
        if (snapshotWriter != null) {
            try {
                snapshotWriter.flush();
            } catch (Exception ignored) {
                // keep loop running even if flush fails
            }
        }
    }


    record AccScore(int accountId, double score, double z500, double z1s, double zqa,
            byte level, double cv, int botScore) { }
}