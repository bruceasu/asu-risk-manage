package me.asu.ta;
import java.util.*;

/**
 * 群体同步检测器
 * 检测多个账户在短时间窗口内同时交易相同品种和方向
 * 用于发现批量机器人操控、协同操纵等行为
 */
public final class SyncDetector {
    private final int bucketMs;  // 时间桶大小（毫秒）
    
    // bucket_start_ms -> symbol+side -> List<account_id>
    private final Map<Long, Map<String, List<Integer>>> buckets = new HashMap<>();
    
    // 记录每个账户参与的同步事件
    private final Map<Integer, Integer> accountSyncCount = new HashMap<>();
    
    /**
     * @param bucketMs 时间桶大小，推荐 500ms
     */
    SyncDetector(int bucketMs) {
        this.bucketMs = bucketMs;
    }
    
    /**
     * 添加一笔订单
     */
    public void onOrder(int accountId, String symbol, byte side, long ts) {
        long bucket = (ts / bucketMs) * bucketMs;
        String key = symbol + "|" + (side > 0 ? "B" : "S");
        
        buckets.computeIfAbsent(bucket, k -> new HashMap<>())
               .computeIfAbsent(key, k -> new ArrayList<>())
               .add(accountId);
        
        // 移除旧桶（保留最近 60 秒）
        long oldestBucket = ts - 60000;
        buckets.keySet().removeIf(b -> b < oldestBucket);
    }
    
    /**
     * 查询账户在时间窗口内参与的同步事件数
     * @param accountId 账户ID
     * @param windowMs 时间窗口（如 60000 = 最近1分钟）
     * @param minAccounts 最少账户数才算同步事件（如 3）
     */
    public int countSyncEvents(int accountId, long nowMs, long windowMs, int minAccounts) {
        long startMs = nowMs - windowMs;
        int count = 0;
        
        for (var entry : buckets.entrySet()) {
            long bucket = entry.getKey();
            if (bucket < startMs || bucket > nowMs) continue;
            
            for (var accounts : entry.getValue().values()) {
                if (accounts.size() >= minAccounts && accounts.contains(accountId)) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 查询账户参与的最大同步组规模
     */
    public int maxSyncGroupSize(int accountId, long nowMs, long windowMs) {
        long startMs = nowMs - windowMs;
        int maxSize = 0;
        
        for (var entry : buckets.entrySet()) {
            long bucket = entry.getKey();
            if (bucket < startMs || bucket > nowMs) continue;
            
            for (var accounts : entry.getValue().values()) {
                if (accounts.contains(accountId) && accounts.size() > maxSize) {
                    maxSize = accounts.size();
                }
            }
        }
        
        return maxSize;
    }
    
    /**
     * 提取同步事件明细（用于持久化或图分析）
     */
    public List<SyncEvent> extractSyncEvents(long startMs, long endMs, int minAccounts) {
        List<SyncEvent> result = new ArrayList<>();
        
        for (var entry : buckets.entrySet()) {
            long bucket = entry.getKey();
            if (bucket < startMs || bucket > endMs) continue;
            
            for (var e2 : entry.getValue().entrySet()) {
                String symbolSide = e2.getKey();
                List<Integer> accounts = e2.getValue();
                
                if (accounts.size() >= minAccounts) {
                    result.add(new SyncEvent(bucket, symbolSide, new ArrayList<>(accounts)));
                }
            }
        }
        
        return result;
    }
    
    /**
     * 同步事件记录
     */
    public static class SyncEvent {
        final long bucketMs;
        final String symbolSide;  // "EURUSD|B"
        final List<Integer> accountIds;
        
        SyncEvent(long bucketMs, String symbolSide, List<Integer> accountIds) {
            this.bucketMs = bucketMs;
            this.symbolSide = symbolSide;
            this.accountIds = accountIds;
        }
        
        int accountCount() {
            return accountIds.size();
        }
        
        String accountIdsString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < accountIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(accountIds.get(i));
            }
            return sb.toString();
        }
    }
}