package me.asu.ta;

import me.asu.ta.util.DoubleArray;import java.math.BigDecimal;

/**
 * 止盈止损模式分析器
 * 检测固定止盈止损设置（机器人策略特征）
 */
public final class TPSLPatternAnalyzer {
    private final DoubleArray tpDistances = new DoubleArray();
    private final DoubleArray slDistances = new DoubleArray();
    private int identicalTPCount = 0;
    private int identicalSLCount = 0;
    private int noTPSLCount = 0;
    private int totalOrders = 0;

    private Double lastTP = null;
    private Double lastSL = null;

    /**
     * 添加订单的止盈止损信息
     * 
     * @param price 订单价格
     * @param takeProfitPrice 止盈价格
     * @param stopLossPrice 止损价格
     */
    public void add(BigDecimal price, BigDecimal takeProfitPrice, BigDecimal stopLossPrice) {
        totalOrders++;

        if (takeProfitPrice == null && stopLossPrice == null) {
            noTPSLCount++;
            return;
        }

        if (price == null)
            return;

        // 计算止盈距离
        if (takeProfitPrice != null) {
            double tpDist = takeProfitPrice.subtract(price).abs().doubleValue();
            tpDistances.add(tpDist);

            // 检测与前一笔是否完全相同
            if (lastTP != null && Math.abs(tpDist - lastTP) < 0.0000001) {
                identicalTPCount++;
            }
            lastTP = tpDist;
        }

        // 计算止损距离
        if (stopLossPrice != null) {
            double slDist = stopLossPrice.subtract(price).abs().doubleValue();
            slDistances.add(slDist);

            if (lastSL != null && Math.abs(slDist - lastSL) < 0.0000001) {
                identicalSLCount++;
            }
            lastSL = slDist;
        }
    }

    /**
     * 平均止盈距离（点数）
     */
    public double avgTPDistance() {
        if (tpDistances.isEmpty())
            return 0;
        double sum = 0;
        for (double d : tpDistances.toArray())
            sum += d;
        return sum / tpDistances.size();
    }

    /**
     * 平均止损距离
     */
    public double avgSLDistance() {
        if (slDistances.isEmpty())
            return 0;
        double sum = 0;
        for (double d : slDistances.toArray())
            sum += d;
        return sum / slDistances.size();
    }

    /**
     * 止盈/止损比例
     */
    public double tpSlRatio() {
        double avgTP = avgTPDistance();
        double avgSL = avgSLDistance();
        return avgSL > 0 ? avgTP / avgSL : 0;
    }

    /**
     * 固定止盈占比（与前一笔完全相同）
     * 可疑阈值：> 0.8
     */
    public double identicalTPRatio() {
        return tpDistances.size() > 1 ? identicalTPCount / (double) (tpDistances.size() - 1) : 0;
    }

    /**
     * 固定止损占比
     */
    public double identicalSLRatio() {
        return slDistances.size() > 1 ? identicalSLCount / (double) (slDistances.size() - 1) : 0;
    }

    /**
     * 固定止盈止损占比（综合）
     */
    public double identicalTPSLRatio() {
        int total = Math.max(tpDistances.size() - 1, 0) + Math.max(slDistances.size() - 1, 0);
        int identical = identicalTPCount + identicalSLCount;
        return total > 0 ? identical / (double) total : 0;
    }

    /**
     * 无止盈止损订单占比
     */
    public double noTPSLRatio() {
        return totalOrders > 0 ? noTPSLCount / (double) totalOrders : 0;
    }

    /**
     * 止盈距离变异系数（CV < 0.1 表示固定模式）
     */
    public double tpCV() {
        if (tpDistances.size() < 2)
            return 0;

        double mean = avgTPDistance();
        double sumSq = 0;
        for (double d : tpDistances.toArray()) {
            double diff = d - mean;
            sumSq += diff * diff;
        }
        double std = Math.sqrt(sumSq / tpDistances.size());

        return mean > 0 ? std / mean : 0;
    }
}