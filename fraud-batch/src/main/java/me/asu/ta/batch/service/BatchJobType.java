package me.asu.ta.batch.service;

public enum BatchJobType {
    FEATURE,
    RISK,
    CASE,
    PIPELINE;

    public static BatchJobType from(String value) {
        return BatchJobType.valueOf(value.trim().toUpperCase());
    }
}
