package me.asu.ta.graph.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Row model mapped to {@code account_graph_signal}.
 * This is the table-aligned implementation of the graph risk signal object.
 *
 * @param accountId primary key account id
 * @param graphWindowStart inclusive graph build window start
 * @param graphWindowEnd exclusive graph build window end
 * @param graphScore final account-level graph score capped to 0-100
 * @param graphClusterSize connected cluster size for the account
 * @param riskNeighborCount one-hop risky neighbor count
 * @param twoHopRiskNeighborCount two-hop risky neighbor count
 * @param sharedDeviceAccounts distinct linked accounts through shared devices
 * @param sharedIpAccounts distinct linked accounts through shared IPs
 * @param sharedBankAccounts distinct linked accounts through shared bank accounts
 * @param collectorAccountFlag whether the account looks like a collector or funnel destination
 * @param funnelInDegree incoming transfer degree used in collector analysis
 * @param funnelOutDegree outgoing transfer degree used in collector analysis
 * @param localDensityScore local neighbor density score capped to 0-100
 * @param clusterRiskScore cluster-level risk score feeding downstream consumers
 * @param generatedAt signal generation time
 */
public record AccountGraphSignal(
        String accountId,
        Instant graphWindowStart,
        Instant graphWindowEnd,
        double graphScore,
        Integer graphClusterSize,
        Integer riskNeighborCount,
        Integer twoHopRiskNeighborCount,
        Integer sharedDeviceAccounts,
        Integer sharedIpAccounts,
        Integer sharedBankAccounts,
        Boolean collectorAccountFlag,
        Integer funnelInDegree,
        Integer funnelOutDegree,
        Double localDensityScore,
        Double clusterRiskScore,
        Instant generatedAt
) {
    public AccountGraphSignal {
        Objects.requireNonNull(accountId, "accountId");
        graphScore = Math.max(0.0d, Math.min(100.0d, graphScore));
        Objects.requireNonNull(graphWindowStart, "graphWindowStart");
        Objects.requireNonNull(graphWindowEnd, "graphWindowEnd");
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
