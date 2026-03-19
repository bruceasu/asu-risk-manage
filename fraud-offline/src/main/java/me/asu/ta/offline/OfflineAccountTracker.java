package me.asu.ta.offline;

import me.asu.ta.ClientFingerprintAnalyzer;
import me.asu.ta.IntervalStats;
import me.asu.ta.OrderSizeAnalyzer;
import me.asu.ta.PriceDeviationTracker;
import me.asu.ta.TPSLPatternAnalyzer;
import me.asu.ta.TimeDiffAnalyzer;
import me.asu.ta.dto.EventText;

import java.math.BigDecimal;
import java.util.*;

/**
 * 离线分析阶段的账户行为追踪器，专注于订单时间间隔、价格偏离、客户端特征等维度的统计和分析。
 * 设计目标：
 * - 累积订单时间戳，计算时间间隔分布和变异系数。
 * - 累积价格偏离数据，计算平均偏离。
 * - 累积客户端特征（IP、登录名、客户端类型），分析多样性。
 * - 累积订单大小和TPSL设置，分析模式和熵值。
 * - 提供综合bot评分接口，结合多个维度的统计结果。
 */
public final class OfflineAccountTracker {

    private final List<Long> orderTimes = new ArrayList<>();
    private final PriceDeviationTracker priceDeviation = new PriceDeviationTracker();
    private final TimeDiffAnalyzer timeDiffAnalyzer = new TimeDiffAnalyzer();
    private final ClientFingerprintAnalyzer clientFingerprint = new ClientFingerprintAnalyzer();
    private final OrderSizeAnalyzer sizeAnalyzer = new OrderSizeAnalyzer();
    private final TPSLPatternAnalyzer tpslPattern = new TPSLPatternAnalyzer();
    private final Set<String> localClientTypes = new HashSet<>();
    private IntervalStats cachedStats = null;
    private boolean statsNeedUpdate = true;

    public void addOrderTime(long ts) {
        orderTimes.add(ts);
        statsNeedUpdate = true;
    }

    public void addEventText(EventText et, double execPrice, String side) {
        if (et != null) {
            if (et.requestPrice != null) {
                priceDeviation.add(et.requestPrice, BigDecimal.valueOf(execPrice));
            }
            clientFingerprint.add(et);
            if (et.clientType != null) {
                localClientTypes.add(et.clientType);
            }
        }
    }

    public void addOrderSize(double size) {
        if (size > 0) {
            sizeAnalyzer.add(BigDecimal.valueOf(size));
        }
    }

    public void addTimeDiff(long diff) {
        timeDiffAnalyzer.add(diff);
    }

    public double getTimeDiffNegativeRatio() {return timeDiffAnalyzer.negativeRatio();}

    public double getTimeDiffExtremeDelayRatio() {return timeDiffAnalyzer.extremeDelayRatio();}

    public double getTimeDiffStd() {
        return timeDiffAnalyzer.std();
    }

    public long getTimeDiffMedian() {
        return timeDiffAnalyzer.median();
    }

    public void addTPSL(Double tp, Double sl, double execPrice, String side) {
        BigDecimal price = BigDecimal.valueOf(execPrice);
        BigDecimal tpBD = tp != null ? BigDecimal.valueOf(tp) : null;
        BigDecimal slBD = sl != null ? BigDecimal.valueOf(sl) : null;
        tpslPattern.add(price, tpBD, slBD);
    }

    public IntervalStats computeStats() {
        if (!statsNeedUpdate && cachedStats != null) {
            return cachedStats;
        }

        if (orderTimes.size() < 2) {
            cachedStats = null;
            statsNeedUpdate = false;
            return null;
        }

        List<Long> sorted = new ArrayList<>(orderTimes);
        Collections.sort(sorted);

        long[] deltas = new long[sorted.size() - 1];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = sorted.get(i + 1) - sorted.get(i);
        }

        cachedStats = IntervalStats.compute(deltas);
        statsNeedUpdate = false;
        return cachedStats;
    }

    public double getEntropy() {
        return sizeAnalyzer.entropy();
    }

    public double getTPSLRatio() {
        return 1.0 - tpslPattern.noTPSLRatio();
    }

    public int getClientIPCount() {
        return clientFingerprint.uniqueIps();
    }

    public String getClientTypes() {
        if (localClientTypes.isEmpty()) {
            return "";
        }
        List<String> sorted = new ArrayList<>(localClientTypes);
        Collections.sort(sorted);
        return String.join("|", sorted);
    }

    public int getLoginNameCount() {
        return clientFingerprint.uniqueLoginNames();
    }

    public double getAvgPriceDeviation() {
        return priceDeviation.avgDeviation();
    }

    public int getTotalOrders() {
        return orderTimes.size();
    }

    public int computeBotScore() {
        IntervalStats stats = computeStats();
        if (stats == null || !stats.isBotLike()) {
            return 0;
        }

        double cv = stats.cv();
        int baseScore;

        if (cv < 0.10) {
            baseScore = 95;
        } else if (cv < 0.15) {
            baseScore = 85;
        } else if (cv < 0.20) {
            baseScore = 70;
        } else {
            baseScore = 50;
        }

        int bonus = 0;
        if (getEntropy() < 0.5) bonus += 10;
        if (getTPSLRatio() > 0.9) bonus += 10;
        if (getClientIPCount() == 1 && getTotalOrders() > 50) bonus += 5;

        return Math.min(100, baseScore + bonus);
    }
}
