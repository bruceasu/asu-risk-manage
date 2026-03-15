package me.asu.ta;

public final class AccountState {
    // === 原有的 Markout 和 Quote Age 统计 ===
    final RollingStats mark500 = new RollingStats();
    final RollingStats mark1s = new RollingStats();
    final RollingStats qAge = new RollingStats();

    // === 风险等级控制 ===
    public byte level = 0;
    public int upCount = 0;
    public int downCount = 0;

    // === 限速控制 ===
    public long rlSec = Long.MIN_VALUE;
    public int rlCount = 0;

    // === 新增：时间间隔追踪（环形缓冲区）===
    private final long[] recentOrderTimes = new long[100];
    private int orderTimeIdx = 0;
    private int orderTimeCount = 0;

    // === 新增：Request 行为追踪器 ===
    final PriceDeviationTracker priceDeviation = new PriceDeviationTracker();
    final TimeDiffAnalyzer timeDiffAnalyzer = new TimeDiffAnalyzer();
    final ClientFingerprintAnalyzer clientFingerprint = new ClientFingerprintAnalyzer();

    // === 新增：订单参数追踪器 ===
    final OrderSizeAnalyzer sizeAnalyzer = new OrderSizeAnalyzer();
    final TPSLPatternAnalyzer tpslPattern = new TPSLPatternAnalyzer();

    // === 新增：订单计数 ===
    int totalOrders = 0;
    int burstCount = 0; // 爆发式交易次数

    /**
     * 添加订单时间戳（用于时间间隔统计）
     */
    public void addOrderTime(long ts) {
        recentOrderTimes[orderTimeIdx] = ts;
        orderTimeIdx = (orderTimeIdx + 1) % recentOrderTimes.length;
        if (orderTimeCount < recentOrderTimes.length)
            orderTimeCount++;
        totalOrders++;
    }

    /**
     * 计算时间间隔统计
     */
    public IntervalStats getIntervalStats() {
        if (orderTimeCount < 2)
            return IntervalStats.EMPTY;

        long[] deltas = new long[orderTimeCount - 1];
        for (int i = 0; i < deltas.length; i++) {
            int curr = (orderTimeIdx - orderTimeCount + i + recentOrderTimes.length)
                    % recentOrderTimes.length;
            int next = (curr + 1) % recentOrderTimes.length;
            deltas[i] = recentOrderTimes[next] - recentOrderTimes[curr];
        }

        return IntervalStats.compute(deltas);
    }

    /**
     * 获取最近 N 秒内的订单数（用于爆发检测）
     */
    public int getOrdersInLastNSeconds(int seconds) {
        if (orderTimeCount == 0)
            return 0;

        long nowMs = recentOrderTimes[(orderTimeIdx - 1 + recentOrderTimes.length)
                % recentOrderTimes.length];
        long thresholdMs = nowMs - seconds * 1000L;

        int count = 0;
        for (int i = 0; i < orderTimeCount; i++) {
            int idx = (orderTimeIdx - orderTimeCount + i + recentOrderTimes.length)
                    % recentOrderTimes.length;
            if (recentOrderTimes[idx] >= thresholdMs) {
                count++;
            }
        }
        return count;
    }
}