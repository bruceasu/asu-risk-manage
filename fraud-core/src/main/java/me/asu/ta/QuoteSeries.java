package me.asu.ta;
// 同一 symbol 的报价时间序列，支持"<=t 最近一笔"查询

import me.asu.ta.dto.QuoteEvent;

import java.util.Comparator;
import java.util.List;

public class QuoteSeries {
    final long[] t;
    final double[] mid;

    /** 构造并按时间升序固化为数组，便于后续二分查询。 */
    public QuoteSeries(List<QuoteEvent> quotes) {
        quotes.sort(Comparator.comparingLong(QuoteEvent::ts));
        t = new long[quotes.size()];
        mid = new double[quotes.size()];
        for (int i = 0; i < quotes.size(); i++) {
            t[i] = quotes.get(i).ts();
            mid[i] = quotes.get(i).mid();
        }
    }

    // 二分查找：返回 ts 之前（含）最后一笔报价下标
    /**
     * 二分查找 `<= ts` 的最后一条报价下标。
     * 算法：标准 upper_bound 变体，循环结束后 hi 即 floor 位置。
     */
    public int floorIndex(long timeMs) {
        if (t.length == 0)
            return -1;
        if (timeMs < t[0])
            return -1;
        if (timeMs >= t[t.length - 1])
            return t.length - 1;
        int lo = 0, hi = t.length - 1;
        while (lo <= hi) {
            int midIdx = (lo + hi) >>> 1;
            long v = t[midIdx];
            if (v <= timeMs)
                lo = midIdx + 1;
            else
                hi = midIdx - 1;
        }
        return hi;
    }

    /** 获取某时刻及之前最近一条报价的 mid。 */
    public Double midAtOrBefore(long timeMs) {
        int idx = floorIndex(timeMs);
        return idx >= 0 ? mid[idx] : null;
    }

    /** 获取某时刻及之前最近一条报价的时间戳。 */
    public Long lastQuoteTimeAtOrBefore(long timeMs) {
        int idx = floorIndex(timeMs);
        return idx >= 0 ? t[idx] : null;
    }
}