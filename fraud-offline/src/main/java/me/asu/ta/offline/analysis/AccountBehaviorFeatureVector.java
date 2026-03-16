package me.asu.ta.offline.analysis;

import java.util.Arrays;

public final class AccountBehaviorFeatureVector {
    private final String accountId;
    private final long tradeCount;
    private final int symbolCount;
    private final double avgHoldingTimeProxy;
    private final double quoteAgeMean;
    private final double quoteAgeP50;
    private final double quoteAgeP90;
    private final double interArrivalCv;
    private final double entropy;
    private final double tpslRatio;
    private final int clientIpCount;
    private final int clientTypeCount;
    private final double avgSize;
    private final double sizeStdLike;
    private final double buySellImbalance;
    private final double markout500Mean;
    private final double markout1sMean;
    private final double[] normalizedVector;
    private final double rawVectorNorm;

    public AccountBehaviorFeatureVector(
            String accountId,
            long tradeCount,
            int symbolCount,
            double avgHoldingTimeProxy,
            double quoteAgeMean,
            double quoteAgeP50,
            double quoteAgeP90,
            double interArrivalCv,
            double entropy,
            double tpslRatio,
            int clientIpCount,
            int clientTypeCount,
            double avgSize,
            double sizeStdLike,
            double buySellImbalance,
            double markout500Mean,
            double markout1sMean,
            double[] normalizedVector,
            double rawVectorNorm) {
        this.accountId = accountId;
        this.tradeCount = tradeCount;
        this.symbolCount = symbolCount;
        this.avgHoldingTimeProxy = avgHoldingTimeProxy;
        this.quoteAgeMean = quoteAgeMean;
        this.quoteAgeP50 = quoteAgeP50;
        this.quoteAgeP90 = quoteAgeP90;
        this.interArrivalCv = interArrivalCv;
        this.entropy = entropy;
        this.tpslRatio = tpslRatio;
        this.clientIpCount = clientIpCount;
        this.clientTypeCount = clientTypeCount;
        this.avgSize = avgSize;
        this.sizeStdLike = sizeStdLike;
        this.buySellImbalance = buySellImbalance;
        this.markout500Mean = markout500Mean;
        this.markout1sMean = markout1sMean;
        this.normalizedVector = normalizedVector;
        this.rawVectorNorm = rawVectorNorm;
    }

    public String getAccountId() {
        return accountId;
    }

    public long getTradeCount() {
        return tradeCount;
    }

    public int getSymbolCount() {
        return symbolCount;
    }

    public double getAvgHoldingTimeProxy() {
        return avgHoldingTimeProxy;
    }

    public double getQuoteAgeMean() {
        return quoteAgeMean;
    }

    public double getQuoteAgeP50() {
        return quoteAgeP50;
    }

    public double getQuoteAgeP90() {
        return quoteAgeP90;
    }

    public double getInterArrivalCv() {
        return interArrivalCv;
    }

    public double getEntropy() {
        return entropy;
    }

    public double getTpslRatio() {
        return tpslRatio;
    }

    public int getClientIpCount() {
        return clientIpCount;
    }

    public int getClientTypeCount() {
        return clientTypeCount;
    }

    public double getAvgSize() {
        return avgSize;
    }

    public double getSizeStdLike() {
        return sizeStdLike;
    }

    public double getBuySellImbalance() {
        return buySellImbalance;
    }

    public double getMarkout500Mean() {
        return markout500Mean;
    }

    public double getMarkout1sMean() {
        return markout1sMean;
    }

    public double[] getNormalizedVector() {
        return Arrays.copyOf(normalizedVector, normalizedVector.length);
    }

    public double getRawVectorNorm() {
        return rawVectorNorm;
    }
}
