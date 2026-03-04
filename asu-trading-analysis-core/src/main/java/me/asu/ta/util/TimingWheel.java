package me.asu.ta.util;

import me.asu.ta.RiskConfig;
import me.asu.ta.Pending;

public final class TimingWheel {
    final PendingArray[] slots = new PendingArray[RiskConfig.WHEEL_SIZE_POW2];
    long curTick = Long.MIN_VALUE;

    public TimingWheel() {
        for (int i = 0; i < slots.length; i++)
            slots[i] = new PendingArray();
    }

    public void schedule(Pending p) {
        long tick = p.dueTs / RiskConfig.TICK_MS;
        int slot = (int) (tick & RiskConfig.WHEEL_MASK);
        slots[slot].add(p);
    }

    public void advanceTo(long nowMs, PendingArray.Settler settler) {
        long targetTick = nowMs / RiskConfig.TICK_MS;
        if (curTick == Long.MIN_VALUE) {
            curTick = targetTick;
        }
        while (curTick <= targetTick) {
            int slot = (int) (curTick & RiskConfig.WHEEL_MASK);
            slots[slot].drainDue(nowMs, settler);
            curTick++;
        }
    }


}