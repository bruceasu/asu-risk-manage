package me.asu.ta;
import java.util.Arrays;

/**
 * 时间间隔统计结果
 * 用于分析账户交易时间间隔的分布特征，检测机器人行为
 */
public record IntervalStats(
    double mean,           // 平均间隔（毫秒）
    double std,            // 标准差
    double cv,             // 变异系数 = std / mean（核心机器人检测指标）
    long min,              // 最小间隔
    long max,              // 最大间隔
    long median,           // 中位数
    double pctLt300ms,     // < 300ms 的订单占比
    double pctLt500ms,     // < 500ms 的订单占比
    double pctLt1s         // < 1s 的订单占比
) {
    static final IntervalStats EMPTY = new IntervalStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
    
    /**
     * 从时间间隔数组计算统计指标
     * @param deltas 时间间隔数组（毫秒）
     */
    static IntervalStats compute(long[] deltas) {
        if (deltas == null || deltas.length == 0) {
            return EMPTY;
        }
        
        // 计算均值
        double sum = 0;
        for (long d : deltas) sum += d;
        double mean = sum / deltas.length;
        
        // 计算标准差
        double sumSq = 0;
        for (long d : deltas) {
            double diff = d - mean;
            sumSq += diff * diff;
        }
        double std = Math.sqrt(sumSq / deltas.length);
        
        // 变异系数（机器人检测核心指标）
        double cv = mean > 0 ? std / mean : 0;
        
        // 最小值和最大值
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long d : deltas) {
            if (d < min) min = d;
            if (d > max) max = d;
        }
        
        // 中位数
        long[] sorted = Arrays.copyOf(deltas, deltas.length);
        Arrays.sort(sorted);
        long median = sorted[deltas.length / 2];
        
        // 分段统计
        int countLt300 = 0, countLt500 = 0, countLt1s = 0;
        for (long d : deltas) {
            if (d < 300) countLt300++;
            if (d < 500) countLt500++;
            if (d < 1000) countLt1s++;
        }
        
        double pctLt300 = countLt300 / (double) deltas.length;
        double pctLt500 = countLt500 / (double) deltas.length;
        double pctLt1s = countLt1s / (double) deltas.length;
        
        return new IntervalStats(mean, std, cv, min, max, median, pctLt300, pctLt500, pctLt1s);
    }
    
    /**
     * 判断是否为机器人特征
     * 判断标准：CV < 0.15（极低变异系数）且有大量快速订单
     */
    boolean isBotLike() {
        return cv < 0.15 && pctLt500ms > 0.5;
    }
    
    /**
     * 判断是否为高频交易模式
     */
    boolean isHighFrequency() {
        return mean < 2000 && pctLt1s > 0.6;
    }
}