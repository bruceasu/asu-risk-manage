package me.asu.ta.util;
public final class SpscRing<T> {
    private final Object[] buf;
    private final int mask;
    private volatile long head = 0;
    private volatile long tail = 0;

    public SpscRing(int capacityPowerOfTwo) {
        int cap = 1;
        while (cap < capacityPowerOfTwo) cap <<= 1;
        this.buf = new Object[cap];
        this.mask = cap - 1;
    }

    public boolean offer(T e) {
        long h = head;
        long next = h + 1;
        if (next - tail > buf.length) return false;
        buf[(int) (h & mask)] = e;
        head = next;
        return true;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        long t = tail;
        if (t == head) return null;
        int idx = (int) (t & mask);
        Object e = buf[idx];
        buf[idx] = null;
        tail = t + 1;
        return (T) e;
    }
}