package me.asu.ta.util;

import me.asu.ta.Pending;

import java.util.Arrays;

public final class PendingArray {
    private Pending[] a = new Pending[64];
    public int n = 0;

    public void add(Pending p) {
        if (n == a.length)
            a = Arrays.copyOf(a, a.length * 2);
        a[n++] = p;
    }


    public void drainDue(long nowMs, Settler settler) {
        int w = 0;
        for (int i = 0; i < n; i++) {
            Pending p = a[i];
            if (p.dueTs <= nowMs) {
                settler.settle(p);
            } else {
                a[w++] = p;
            }
        }
        for (int i = w; i < n; i++)
            a[i] = null;
        n = w;
    }


    public interface Settler {
        void settle(Pending p);
    }
}