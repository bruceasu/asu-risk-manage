package me.asu.ta;

public final class RiskConfig {
    private RiskConfig() {}

    public static final long TICK_MS = 10;
    public static final int WHEEL_SIZE_POW2 = 131072;
    public static final int WHEEL_MASK = WHEEL_SIZE_POW2 - 1;

    public static final int MAX_SYMBOLS = 256;
    public static final int MAX_ACCOUNTS = 40960;
    public static final int TOP_N = 10;

    public static final int ROLL_BUCKET_MS = 1000;
    public static final int ROLL_WIN_SEC = 15 * 60;

    public static final double W_Z500 = 0.4;   // 调整为 0.4
    public static final double W_Z1S = 0.2;    // 调整为 0.2
    public static final double W_ZQA = 0.1;
    
    public static final double W_CV = 0.15;     // 时间间隔变异系数
    public static final double W_TPSL = 0.1;    // 固定止盈止损模式
    public static final double W_CLIENT = 0.05; // 客户端指纹异常

    public static final double TH_L1 = 2.0;
    public static final double TH_L2 = 3.0;
    public static final double TH_L3 = 5.0;

    public static final int ENTER_K = 3;
    public static final int EXIT_M = 6;

    public static final int L0_MAX_OPS = 50;
    public static final int L1_MAX_OPS = 20;
    public static final int L2_MAX_OPS = 10;
    public static final int L3_MAX_OPS = 5;
    
    // === 同步检测参数 ===
    public static final int SYNC_BUCKET_MS = 500;     // 同步时间桶大小
    public static final int SYNC_MIN_ACCOUNTS = 3;    // 最少账户数才算同步
    
    /**
     * 计算增强的风险评分
     * 整合 markout、时间间隔、订单模式等多维度特征
     * 
     * @param acc 账户状态
     * @param globalMark500Mean 全局 500ms markout 均值
     * @param globalMark500Std 全局 500ms markout 标准差
     * @param globalMark1sMean 全局 1s markout 均值
     * @param globalMark1sStd 全局 1s markout 标准差
     * @param globalQAgeMean 全局 quote age 均值
     * @param globalQAgeStd 全局 quote age 标准差
     * @return 综合风险评分
     */
    static double computeEnhancedScore(
            AccountState acc,
            double globalMark500Mean, double globalMark500Std,
            double globalMark1sMean, double globalMark1sStd,
            double globalQAgeMean, double globalQAgeStd) {
        
        // 1. Markout 维度（原有逻辑）
        double zMark500 = zscore(acc.mark500.mean(), globalMark500Mean, globalMark500Std);
        double zMark1s = zscore(acc.mark1s.mean(), globalMark1sMean, globalMark1sStd);
        double zQA = zscore(acc.qAge.mean(), globalQAgeMean, globalQAgeStd);
        
        // 2. 时间间隔维度（新增）
        IntervalStats ivStats = acc.getIntervalStats();
        double cvScore = 0;
        if (ivStats.cv() < 0.10) {
            cvScore = 5.0;  // 极低 CV = 强机器人特征
        } else if (ivStats.cv() < 0.15) {
            cvScore = 3.0;
        } else if (ivStats.cv() < 0.25) {
            cvScore = 1.5;
        }
        
        // 加上超快订单占比
        if (ivStats.pctLt300ms() > 0.7) {
            cvScore += 3.0;
        } else if (ivStats.pctLt500ms() > 0.5) {
            cvScore += 1.5;
        }
        
        // 3. 固定策略维度（新增）
        double tpslScore = 0;
        if (acc.tpslPattern.identicalTPSLRatio() > 0.8) {
            tpslScore = 3.0;
        } else if (acc.tpslPattern.identicalTPSLRatio() > 0.6) {
            tpslScore = 1.5;
        }
        
        if (acc.sizeAnalyzer.entropy() < 1.0) {
            tpslScore += 2.0;
        }
        
        // 4. 客户端指纹维度（新增）
        double clientScore = 0;
        if (acc.clientFingerprint.hasBotKeyword()) {
            clientScore = 10.0;  // 直接定性
        } else {
            if (acc.clientFingerprint.uniqueLoginNames() > 1) {
                clientScore += 2.0;
            }
            if (acc.priceDeviation.zeroDeviationRatio() > 0.9) {
                clientScore += 2.0;
            }
        }
        
        // 综合评分
        double score = W_Z500 * zMark500 
                     + W_Z1S * zMark1s 
                     + W_ZQA * zQA 
                     + W_CV * cvScore 
                     + W_TPSL * tpslScore 
                     + W_CLIENT * clientScore;
        
        return score;
    }
    
    /**
     * 计算 Z-score
     */
    private static double zscore(double value, double mean, double std) {
        if (std < 1e-9) return 0.0;
        return (value - mean) / std;
    }
    
    /**
     * 计算机器人可能性评分 (0-100分)
     * 用于离线分析或详细报告
     */
    static double computeBotLikelihoodScore(AccountState acc) {
        double score = 0;
        
        // 1. 时间规律性（0-25分）
        IntervalStats iv = acc.getIntervalStats();
        if (iv.cv() < 0.10) score += 15;
        else if (iv.cv() < 0.20) score += 10;
        else if (iv.cv() < 0.30) score += 5;
        
        if (iv.pctLt300ms() > 0.7) score += 10;
        else if (iv.pctLt500ms() > 0.5) score += 5;
        
        // 2. 固定策略（0-20分）
        if (acc.tpslPattern.identicalTPSLRatio() > 0.8) score += 10;
        else if (acc.tpslPattern.identicalTPSLRatio() > 0.6) score += 5;
        
        if (acc.sizeAnalyzer.entropy() < 1.0) score += 10;
        else if (acc.sizeAnalyzer.entropy() < 1.5) score += 5;
        
        // 3. Request 异常（0-15分）
        if (acc.priceDeviation.zeroDeviationRatio() > 0.9) score += 8;
        if (acc.timeDiffAnalyzer.negativeRatio() > 0.5) score += 5;
        if (acc.clientFingerprint.hasBotKeyword()) score += 15;
        
        // 4. 客户端指纹（0-10分）
        if (acc.clientFingerprint.uniqueLoginNames() > 1) score += 5;
        if (acc.clientFingerprint.uniqueClientTypes() > 3) score += 5;
        
        return Math.min(score, 100.0);
    }
}