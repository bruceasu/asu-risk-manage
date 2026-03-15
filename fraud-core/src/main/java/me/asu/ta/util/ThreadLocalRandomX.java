package me.asu.ta.util;
// ======= Small RNG helper (no java.util.concurrent.ThreadLocalRandom in older builds) =======
public class ThreadLocalRandomX {
    /**
     * 生成 [0, boundExclusive) 的伪随机 long。
     * 算法：xorshift64*，速度快、实现简单；用于采样替换场景。
     */
    public static long nextLong(long boundExclusive) {
        // xorshift64*
        long x = System.nanoTime()
                ^ (System.identityHashCode(ThreadLocalRandomX.class) * 0x9E3779B97F4A7C15L);
        x ^= (x >>> 12);
        x ^= (x << 25);
        x ^= (x >>> 27);
        long r = x * 2685821657736338717L;
        long u = r >>> 1; // positive
        return boundExclusive <= 0 ? 0 : (u % boundExclusive);
    }
}