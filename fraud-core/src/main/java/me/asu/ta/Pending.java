package me.asu.ta;
public final class Pending {
    public int accountId;
    public int symbolId;
    public long dueTs;
    public double mid0;
    public byte side;
    public short deltaIdx; // 0=500ms, 1=1s
}