package me.asu.ta.util;
// ======= Simple bounded primitive long array =======

import java.util.Arrays;

public class DoubleArray  {
    double[] a = new double[16];
    int size = 0;

    /** 追加元素，容量不足时按 2 倍扩容。 */
    public void add(double v) {
        if (size == a.length)
            a = Arrays.copyOf(a, a.length * 2);
        a[size++] = v;
    }

    /** 原位覆盖指定下标元素。 */
    public void set(int i, double v) {
        if (i >= size) throw new IndexOutOfBoundsException();
        a[i] = v;
    }

    public double get(int i) {
        if (i >= size) throw new IndexOutOfBoundsException();
        return a[i];
    }

    public int size() {
        return size;
    }
    /** 导出当前有效元素副本。 */
    public double[] toArray() {
        return Arrays.copyOf(a, size);
    }

    public boolean isEmpty() {
        return size == 0;
    }

}