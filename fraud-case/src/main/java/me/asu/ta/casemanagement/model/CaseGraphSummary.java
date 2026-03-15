package me.asu.ta.casemanagement.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Graph summary mapped to {@code case_graph_summary}.
 */
public record CaseGraphSummary(
        long caseId,
        Double graphScore,
        Integer graphClusterSize,
        Integer riskNeighborCount,
        Integer sharedDeviceAccounts,
        Integer sharedBankAccounts,
        Instant createdAt
) {
    public CaseGraphSummary {
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
