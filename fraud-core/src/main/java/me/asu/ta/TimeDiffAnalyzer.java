package me.asu.ta;

import me.asu.ta.util.CommonUtils;

/**
 * 时间差分析器 (简化版)
 * 分析 eventText 中的 td（timeDiff = 服务器时间 - 客户端时间）
 * 检测客户端时钟异常、重放攻击、固定延迟模拟客户端
 * 
 * 注意：不再依赖 LongSamples，使用简化的统计方法
 */
public final class TimeDiffAnalyzer {
    private long count = 0;
    private long sumDiff = 0;
    private long sumSqDiff = 0;
    private int negativeCount = 0;
    private int extremeDelayCount = 0;  // > 1000ms
    
    // 简化的样本缓冲区（用于中位数计算）
    private final long[] buffer = new long[1000];
    private int bufferIndex = 0;
    
    /**
     * 添加一笔时间差样本
     * @param timeDiff 服务器时间 - 客户端时间（毫秒）
     *                 正值表示客户端慢，负值表示客户端快
     */
    public void add(long timeDiff) {
        count++;
        sumDiff += timeDiff;
        sumSqDiff += timeDiff * timeDiff;
        
        if (timeDiff < 0) {
            negativeCount++;
        }
        
        if (timeDiff > 1000) {
            extremeDelayCount++;
        }
        
        // 添加到循环缓冲区
        buffer[bufferIndex % buffer.length] = timeDiff;
        bufferIndex++;
    }
    
    /**
     * 中位数时间差
     */
    public long median() {
        if (bufferIndex == 0) return 0;
        
        int validCount = Math.min(bufferIndex, buffer.length);
        long[] snapshot = new long[validCount];
        System.arraycopy(buffer, 0, snapshot, 0, validCount);
        java.util.Arrays.sort(snapshot);
        
        return snapshot[validCount / 2];
    }
    
    /**
     * 负时间差占比（客户端时钟快于服务器）
     * 可疑阈值：> 0.5
     */
    public double negativeRatio() {
        return count > 0 ? negativeCount / (double) count : 0;
    }
    
    /**
     * 极端延迟占比（> 1秒）
     * 可疑阈值：> 0.2
     */
    public double extremeDelayRatio() {
        return count > 0 ? extremeDelayCount / (double) count : 0;
    }
    
    /**
     * 时间差标准差（用于检测固定延迟模拟客户端）
     */
    public double std() {
        return CommonUtils.std(sumDiff, sumSqDiff, count);
    }
    
    public int getCount() {
        return (int) count;
    }
}