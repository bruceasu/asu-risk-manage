package me.asu.ta;

import me.asu.ta.util.CommonUtils;

/**
 * 全局基线统计信息容器。
 * 避免在多个地方重复计算相同的基线指标。
 */
public class BaselineStats {
    public final double mean100;
    public final double std100;
    public final double mean500;
    public final double std500;
    public final double mean1s;
    public final double std1s;
    public final double mean5s;
    public final double std5s;
    public final double meanQA;
    public final double stdQA;

    public BaselineStats(Agg global) {
        this.mean100 = CommonUtils.mean(global.sumMark100, global.n);
        this.std100 = CommonUtils.std(global.sumMark100, global.sumSqMark100, global.n);
        this.mean500 = CommonUtils.mean(global.sumMark500, global.n);
        this.std500 = CommonUtils.std(global.sumMark500, global.sumSqMark500, global.n);
        this.mean1s = CommonUtils.mean(global.sumMark1s, global.n);
        this.std1s = CommonUtils.std(global.sumMark1s, global.sumSqMark1s, global.n);
        this.mean5s = CommonUtils.mean(global.sumMark5s, global.n);
        this.std5s = CommonUtils.std(global.sumMark5s, global.sumSqMark5s, global.n);
        this.meanQA = CommonUtils.mean(global.sumQuoteAge, global.n);
        this.stdQA = CommonUtils.std(global.sumQuoteAge, global.sumSqQuoteAge, global.n);
    }
}
