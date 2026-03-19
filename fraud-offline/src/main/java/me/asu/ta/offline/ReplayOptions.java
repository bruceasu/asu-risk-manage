package me.asu.ta.offline;

import java.util.Locale;

public final class ReplayOptions {
    private final int bucketMin;
    private final String bucketBy;
    private final boolean quoteAgeStats;
    private final String quoteAgeScope;
    private final int quoteAgeMaxSamplesPerKey;

    ReplayOptions(
            int bucketMin,
            String bucketBy,
            boolean quoteAgeStats,
            String quoteAgeScope,
            int quoteAgeMaxSamplesPerKey) {
        this.bucketMin = bucketMin;
        this.bucketBy = normalize(bucketBy, "all");
        this.quoteAgeStats = quoteAgeStats;
        this.quoteAgeScope = normalize(quoteAgeScope, "all");
        this.quoteAgeMaxSamplesPerKey = quoteAgeMaxSamplesPerKey;
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }

    public int getBucketMin() {
        return bucketMin;
    }

    public String getBucketBy() {
        return bucketBy;
    }

    public boolean isQuoteAgeStats() {
        return quoteAgeStats;
    }

    public String getQuoteAgeScope() {
        return quoteAgeScope;
    }

    public int getQuoteAgeMaxSamplesPerKey() {
        return quoteAgeMaxSamplesPerKey;
    }
}
