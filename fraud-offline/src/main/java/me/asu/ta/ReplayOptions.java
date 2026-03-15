package me.asu.ta;

import java.util.Locale;

final class ReplayOptions {
    final int bucketMin;
    final String bucketBy;
    final boolean quoteAgeStats;
    final String quoteAgeScope;
    final int quoteAgeMaxSamplesPerKey;

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
}
