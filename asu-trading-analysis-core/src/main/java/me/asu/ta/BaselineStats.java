package me.asu.ta;

/**
 * 全局基线统计信息容器。
 * 避免在多个地方重复计算相同的基线指标。
 */
public class BaselineStats {
    final double mean100;
    final double std100;
    final double mean500;
    final double std500;
    final double mean1s;
    final double std1s;
    final double mean5s;
    final double std5s;
    final double meanQA;
    final double stdQA;

    public BaselineStats(Agg global) {
        this.mean100 = me.asu.ta.util.CommonUtils.mean(global.sumMark100, global.n);
        this.std100 = me.asu.ta.util.CommonUtils.std(global.sumMark100, global.sumSqMark100, global.n);
        this.mean500 = me.asu.ta.util.CommonUtils.mean(global.sumMark500, global.n);
        this.std500 = me.asu.ta.util.CommonUtils.std(global.sumMark500, global.sumSqMark500, global.n);
        this.mean1s = me.asu.ta.util.CommonUtils.mean(global.sumMark1s, global.n);
        this.std1s = me.asu.ta.util.CommonUtils.std(global.sumMark1s, global.sumSqMark1s, global.n);
        this.mean5s = me.asu.ta.util.CommonUtils.mean(global.sumMark5s, global.n);
        this.std5s = me.asu.ta.util.CommonUtils.std(global.sumMark5s, global.sumSqMark5s, global.n);
        this.meanQA = me.asu.ta.util.CommonUtils.mean(global.sumQuoteAge, global.n);
        this.stdQA = me.asu.ta.util.CommonUtils.std(global.sumQuoteAge, global.sumSqQuoteAge, global.n);
    }
}