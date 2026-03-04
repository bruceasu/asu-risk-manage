package me.asu.ta;

/**
 * 异常账户评分结果，用于报告输出。
 */
public final class Anomaly {
    public final String account;
    public final double z500;
    public final double z1s;
    public final double zQA;
    public final long trades;

    /**
     * 创建一条账户异常评分记录。
     */
    public Anomaly(String account, double z500, double z1s, double zQA, long trades) {
        this.account = account;
        this.z500 = z500;
        this.z1s = z1s;
        this.zQA = zQA;
        this.trades = trades;
    }
}