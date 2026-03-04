package me.asu.ta;
// ======= Replay outputs =======
// 明细输出行：记录每笔成交与多个 horizon 的 markout

class DetailRow {
    // ==============================================
    static final long[] DELTAS_MS = {100, 500, 1000, 5000};
    static final String[] DELTA_NAMES = {"100ms", "500ms", "1s", "5s"};

    final String account, symbol, side;
    final long execTimeMs;
    final double size;
    final double mid0;
    final long lastQuoteT0;
    final long quoteAgeMs;
    final Double[] mids = new Double[DELTAS_MS.length];
    final Double[] marks = new Double[DELTAS_MS.length];
    
    // 新增：bot检测指标（账户级别）
    Double cv;              // 时间间隔变异系数
    Integer botScore;       // bot综合评分 0-100
    Double entropy;         // 订单大小熵
    Double tpslRatio;       // TP/SL使用比例
    Integer clientIPCount;  // 客户端IP数量
    String clientTypes;     // 客户端类型列表

    DetailRow(String account, String symbol, String side, long execTimeMs, double size,
            double mid0, long lastQuoteT0, long quoteAgeMs) {
        this.account = account;
        this.symbol = symbol;
        this.side = side;
        this.execTimeMs = execTimeMs;
        this.size = size;
        this.mid0 = mid0;
        this.lastQuoteT0 = lastQuoteT0;
        this.quoteAgeMs = quoteAgeMs;
    }
    
    /**
     * 设置bot检测指标（从OfflineAccountTracker计算后填充）。
     */
    void setBotIndicators(IntervalStats stats, double entropy, double tpslRatio,
                         int ipCount, String types) {
        if (stats != null) {
            this.cv = stats.cv();
            this.botScore = stats.isBotLike() ? 
                Math.min(100, (int)(stats.cv() < 0.10 ? 95 : 
                               stats.cv() < 0.15 ? 85 : 70)) : 0;
        }
        this.entropy = entropy;
        this.tpslRatio = tpslRatio;
        this.clientIPCount = ipCount;
        this.clientTypes = types;
    }
}