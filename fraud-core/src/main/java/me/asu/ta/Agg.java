package me.asu.ta;

// 单个分组（account/account|symbol/全局）的统计累加器

import java.util.HashSet;
import java.util.Set;

public class Agg {
    // 样本数（成交笔数）
    public long n = 0;
    // 不同窗口的 markout 总和
    // 用于算平均值：avg_markout_xxx = sumMarkX / n
    public double sumMark100 = 0;
    public double sumMark500 = 0;
    public double sumMark1s = 0;
    public double sumMark5s = 0;

    // 平方和用于算标准差/波动（后续 z-score）
    public double sumQuoteAge = 0;
    public double sumSqMark100 = 0;
    public double sumSqMark500 = 0;
    public double sumSqMark1s = 0;
    public double sumSqMark5s = 0;
    public double sumSqQuoteAge = 0;
    // 对应窗口里 markout > 0 的笔数
    // 用于算正收益比例：pos_ratio_xxx = posX / n 
    public long pos100 = 0;
    public long pos500 = 0;
    public long pos1s = 0;
    public long pos5s = 0;
    // 该分组最早/最晚成交时间
    // 可用于算交易密度（如 trades per min）
    public long minT = Long.MAX_VALUE;
    public long maxT = Long.MIN_VALUE;

    // for clustering extras
    // 该分组涉及过的品种集合
    // 常用于 symbol_count 和聚类特征
    public final Set<String> symbols = new HashSet<>();

    // 每处理一笔成交，就把该笔在不同窗口的指标累加进来

    /**
     * 累加一笔成交的统计量到当前分组。
     * 说明：仅在对应 markout 非空时才计入该窗口，避免未来价格缺失导致误差。
     */
    public void add(String symbol, long execTimeMs, Double m100, Double m500, Double m1s, Double m5s,
            long quoteAgeMs) {
        n++;
        sumQuoteAge += quoteAgeMs;
        sumSqQuoteAge += quoteAgeMs * quoteAgeMs;
        if (m100 != null) {
            sumMark100 += m100;
            sumSqMark100 += m100 * m100;
            if (m100 > 0)
                pos100++;
        }
        if (m500 != null) {
            sumMark500 += m500;
            sumSqMark500 += m500 * m500;
            if (m500 > 0)
                pos500++;
        }
        if (m1s != null) {
            sumMark1s += m1s;
            sumSqMark1s += m1s * m1s;
            if (m1s > 0)
                pos1s++;
        }
        if (m5s != null) {
            sumMark5s += m5s;
            sumSqMark5s += m1s * m5s;
            if (m5s > 0)
                pos5s++;
        }
        minT = Math.min(minT, execTimeMs);
        maxT = Math.max(maxT, execTimeMs);
        if (symbol != null && !symbol.isBlank())
            symbols.add(symbol);
    }
}