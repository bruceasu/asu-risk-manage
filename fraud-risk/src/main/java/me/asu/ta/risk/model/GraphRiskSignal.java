package me.asu.ta.risk.model;

/**
 * Aggregated graph risk inputs already normalized or derived by upstream graph processing.
 */
public record GraphRiskSignal(
        double graphScore,
        int graphClusterSize,
        int riskNeighborCount,
        int sharedDeviceAccounts,
        int sharedBankAccounts
) {
    public GraphRiskSignal {
        graphScore = clamp(graphScore);
        graphClusterSize = Math.max(0, graphClusterSize);
        riskNeighborCount = Math.max(0, riskNeighborCount);
        sharedDeviceAccounts = Math.max(0, sharedDeviceAccounts);
        sharedBankAccounts = Math.max(0, sharedBankAccounts);
    }

    public static GraphRiskSignal empty() {
        return new GraphRiskSignal(0.0d, 0, 0, 0, 0);
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(100.0d, value));
    }
}
