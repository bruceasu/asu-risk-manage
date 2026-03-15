package me.asu.ta;

// ======= QuoteAge percentile stats =======

import me.asu.ta.util.LongArray;
import me.asu.ta.util.ThreadLocalRandomX;

import java.util.Arrays;

public class LongSamples {
    final int max;
    final LongArray data;

    /** 初始化有界样本容器，并设置最小容量下限。 */
    public LongSamples(int max) {
        this.max = Math.max(10_000, max);
        this.data = new LongArray();
    }

    /**
     * 添加样本（有界采样）。
     * 算法：前 max 条直接保留；之后采用 reservoir-like 随机替换，
     * 使旧样本有机会被新样本替换，控制内存上限。
     */
    public void add(long v) {
        // bounded: reservoir-like (simple): keep first max then random replace
        if (data.size < max)
            data.add(v);
        else {
            long i = ThreadLocalRandomX.nextLong(data.size + 1L);
            if (i < max)
                data.set((int) i, v);
        }
    }

    /** 导出样本快照并排序，供分位数计算。 */
    public long[] snapshotSorted() {
        long[] arr = data.toArray();
        Arrays.sort(arr);
        return arr;
    }
}
