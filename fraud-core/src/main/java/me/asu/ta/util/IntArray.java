package me.asu.ta.util;
// ======= Simple bounded primitive long array =======

import java.util.Arrays;

public class IntArray {
    int[] a = new int[16];
    public int size = 0;

    /** 追加元素，容量不足时按 2 倍扩容。 */
    public void add(int v) {
        if (size == a.length)
            a = Arrays.copyOf(a, a.length * 2);
        a[size++] = v;
    }

    /** 原位覆盖指定下标元素。 */
    public void set(int i, int v) {
        if (i >= size) throw new IndexOutOfBoundsException();
        a[i] = v;
    }

    /** 导出当前有效元素副本。 */
    public int[] toArray() {
        return Arrays.copyOf(a, size);
    }
}