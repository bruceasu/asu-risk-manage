package me.asu.ta;

import me.asu.ta.dto.Event;
import me.asu.ta.util.SpscRing;

public class RiskEngineConfig {
    public SpscRing<Event> ring;
    public int symbolCount;
    public int accountCount;
    public SnapshotFileWriter snapshotWriter;
    public int riskMinN;
    
}