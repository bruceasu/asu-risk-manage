package me.asu.ta;
public record ExecutionPolicy(int extraDelayMs, boolean lastLook, int maxOpsPerSecond) {}