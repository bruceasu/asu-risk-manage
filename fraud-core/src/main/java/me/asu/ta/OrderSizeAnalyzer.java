package me.asu.ta;
import java.math.BigDecimal;
import java.util.*;

/**
 * 订单大小分析器
 * 分析订单大小的分布特征，检测固定大小模式（机器人特征）
 */
public final class OrderSizeAnalyzer {
    private final Map<BigDecimal, Integer> sizeFrequency = new HashMap<>();
    private final List<Double> sizes = new ArrayList<>();
    private int integerSizeCount = 0;
    
    /**
     * 添加订单大小
     */
    public void add(BigDecimal size) {
        if (size == null || size.compareTo(BigDecimal.ZERO) <= 0) return;
        
        sizeFrequency.merge(size, 1, Integer::sum);
        sizes.add(size.doubleValue());
        
        // 检测整数大小（如 1.0, 2.0, 10.0）
        if (size.stripTrailingZeros().scale() <= 0) {
            integerSizeCount++;
        }
    }
    
    /**
     * 订单大小熵（香农熵）
     * 熵越低表示分布越集中（固定模式）
     * 可疑阈值：< 1.0
     */
    public double entropy() {
        if (sizes.isEmpty()) return 0;
        
        double entropy = 0.0;
        int total = sizes.size();
        
        for (int count : sizeFrequency.values()) {
            double p = count / (double) total;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    /**
     * 最常见大小占比
     * 可疑阈值：> 0.8
     */
    public double mostCommonSizeRatio() {
        if (sizes.isEmpty()) return 0;
        
        int maxCount = 0;
        for (int count : sizeFrequency.values()) {
            if (count > maxCount) maxCount = count;
        }
        
        return maxCount / (double) sizes.size();
    }
    
    /**
     * 整数大小占比
     * 可疑阈值：> 0.9
     */
    public double integerSizeRatio() {
        return sizes.isEmpty() ? 0 : integerSizeCount / (double) sizes.size();
    }
    
    /**
     * 订单大小变异系数
     * 可疑阈值：< 0.2（固定大小模式）
     */
    public double cv() {
        if (sizes.size() < 2) return 0;
        
        double sum = 0;
        for (double s : sizes) sum += s;
        double mean = sum / sizes.size();
        
        double sumSq = 0;
        for (double s : sizes) {
            double diff = s - mean;
            sumSq += diff * diff;
        }
        double std = Math.sqrt(sumSq / sizes.size());
        
        return mean > 0 ? std / mean : 0;
    }
    
    public int getCount() {
        return sizes.size();
    }
}