package me.asu.ta;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价格偏差追踪器
 * 分析请求价格 vs 实际成交价的偏差，检测陈旧报价套利和模拟盘
 */
public final class PriceDeviationTracker {
    private double sumDeviation = 0;
    private int count = 0;
    private int zeroDeviationCount = 0;
    private int highDeviationCount = 0;  // 偏差 > 0.001 (10 pips on 5-digit)
    
    /**
     * 添加一笔订单的价格偏差
     * 
     * @param requestPrice 客户端请求价格
     * @param execPrice 实际成交价格
     */
    public void add(BigDecimal requestPrice, BigDecimal execPrice) {
        if (requestPrice == null || execPrice == null) return;
        if (requestPrice.compareTo(BigDecimal.ZERO) == 0) return;
        
        try {
            BigDecimal diff = execPrice.subtract(requestPrice).abs();
            BigDecimal deviation = diff.divide(requestPrice, 6, RoundingMode.HALF_UP);
            
            sumDeviation += deviation.doubleValue();
            count++;
            
            // 零偏差检测（可能是模拟盘）
            if (deviation.compareTo(new BigDecimal("0.0000001")) < 0) {
                zeroDeviationCount++;
            }
            
            // 高偏差检测（陈旧报价）
            if (deviation.compareTo(new BigDecimal("0.001")) > 0) {
                highDeviationCount++;
            }
        } catch (ArithmeticException e) {
            // 除法异常，忽略此笔
        }
    }
    
    /**
     * 平均价格偏差
     */
    public double avgDeviation() {
        return count > 0 ? sumDeviation / count : 0;
    }
    
    /**
     * 零偏差订单占比（可疑指标：>0.9）
     */
    public double zeroDeviationRatio() {
        return count > 0 ? zeroDeviationCount / (double) count : 0;
    }
    
    /**
     * 高偏差订单占比（可疑指标：>0.2）
     */
    public double highDeviationRatio() {
        return count > 0 ? highDeviationCount / (double) count : 0;
    }
    
    public int getCount() {
        return count;
    }
}