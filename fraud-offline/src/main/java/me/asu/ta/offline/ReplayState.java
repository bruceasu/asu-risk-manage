package me.asu.ta.offline;

// 回放阶段的全部中间结果容器，避免重复扫描 trades

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.asu.ta.Agg;
import me.asu.ta.DetailRow;
import me.asu.ta.LongSamples;

public class ReplayState {
    public final Agg globalAgg = new Agg();
    public final List<DetailRow> detailRows = new ArrayList<>(200_000);
    public final Map<String, Agg> aggByAccountSymbol = new HashMap<>();
    public final Map<String, Agg> aggByAccount = new HashMap<>();
    // time buckets
    public final Map<String, Agg> buckets = new HashMap<>();
    // quoteAge samples for percentile
    public final Map<String, LongSamples> quoteAgeSamples = new HashMap<>();
    
    // 新增：存储每个账户的bot检测追踪器
    public final Map<String, OfflineAccountTracker> accountTrackers = new HashMap<>();

    public Agg getGlobalAgg() { return globalAgg; }
    public List<DetailRow> getDetailRows() { return detailRows; }
    public Map<String, Agg> getAggByAccountSymbol() { return aggByAccountSymbol; }
    public Map<String, Agg> getAggByAccount() { return aggByAccount; }
    public Map<String, Agg> getBuckets() { return buckets; }
    public Map<String, LongSamples> getQuoteAgeSamples() { return quoteAgeSamples; }
    public Map<String, OfflineAccountTracker> getAccountTrackers() { return accountTrackers; }
}
