package me.asu.ta.offline.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.asu.ta.Agg;
import me.asu.ta.DetailRow;
import me.asu.ta.IntervalStats;
import me.asu.ta.offline.OfflineAccountTracker;
import me.asu.ta.offline.ReplayState;
import me.asu.ta.util.CommonUtils;

public final class BehaviorFeatureAnalysisService {
    public Map<String, AccountBehaviorFeatureVector> analyze(ReplayState state, int minTrades) {
        Map<String, List<DetailRow>> detailsByAccount = new LinkedHashMap<>();
        for (DetailRow row : state.getDetailRows()) {
            detailsByAccount.computeIfAbsent(row.account, key -> new ArrayList<>()).add(row);
        }

        Map<String, AccountBehaviorFeatureVector> result = new LinkedHashMap<>();
        state.getAggByAccount().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String accountId = entry.getKey();
                    Agg agg = entry.getValue();
                    if (agg.n < minTrades) {
                        return;
                    }
                    AccountBehaviorFeatureVector vector = toVector(
                            accountId,
                            agg,
                            detailsByAccount.getOrDefault(accountId, List.of()),
                            state.getAccountTrackers().get(accountId));
                    if (vector != null) {
                        result.put(accountId, vector);
                    }
                });
        return result;
    }

    private AccountBehaviorFeatureVector toVector(
            String accountId,
            Agg agg,
            List<DetailRow> rows,
            OfflineAccountTracker tracker) {
        if (agg == null || agg.n <= 0) {
            return null;
        }
        QuoteAgeStats quoteAgeStats = computeQuoteAgeStats(rows);
        IntervalStats intervalStats = tracker != null ? tracker.computeStats() : null;
        double avgSize = rows.stream().mapToDouble(row -> row.size).filter(value -> value > 0).average().orElse(0.0);
        double sizeStdLike = computeSizeStdLike(rows, avgSize);
        double buySellImbalance = computeBuySellImbalance(rows);
        double markout500Mean = rows.stream()
                .map(row -> row.marks[1])
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double markout1sMean = rows.stream()
                .map(row -> row.marks[2])
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double avgHoldingTimeProxy = intervalStats != null ? intervalStats.mean() : 0.0;
        double interArrivalCv = intervalStats != null ? intervalStats.cv() : 0.0;
        double entropy = tracker != null ? tracker.getEntropy() : 0.0;
        double tpslRatio = tracker != null ? tracker.getTPSLRatio() : 0.0;
        int clientIpCount = tracker != null ? tracker.getClientIPCount() : 0;
        int clientTypeCount = countClientTypes(tracker != null ? tracker.getClientTypes() : "");

        double[] raw = new double[] {
                scale(agg.n, 50.0),
                scale(agg.symbols.size(), 10.0),
                scale(avgHoldingTimeProxy, 1_000.0),
                scale(quoteAgeStats.mean(), 100.0),
                scale(quoteAgeStats.p50(), 100.0),
                scale(quoteAgeStats.p90(), 100.0),
                interArrivalCv,
                entropy,
                tpslRatio,
                scale(clientIpCount, 5.0),
                scale(clientTypeCount, 5.0),
                scale(avgSize, 10.0),
                scale(sizeStdLike, 10.0),
                buySellImbalance,
                scale(markout500Mean, 0.001),
                scale(markout1sMean, 0.001)
        };
        double norm = CommonUtils.l2(raw);
        if (norm == 0.0) {
            return null;
        }
        double[] normalized = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            normalized[i] = raw[i] / norm;
        }
        return new AccountBehaviorFeatureVector(
                accountId,
                agg.n,
                agg.symbols.size(),
                avgHoldingTimeProxy,
                quoteAgeStats.mean(),
                quoteAgeStats.p50(),
                quoteAgeStats.p90(),
                interArrivalCv,
                entropy,
                tpslRatio,
                clientIpCount,
                clientTypeCount,
                avgSize,
                sizeStdLike,
                buySellImbalance,
                markout500Mean,
                markout1sMean,
                normalized,
                norm);
    }

    private double computeSizeStdLike(List<DetailRow> rows, double avgSize) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        double sumSq = 0.0;
        int count = 0;
        for (DetailRow row : rows) {
            if (row.size <= 0) {
                continue;
            }
            double diff = row.size - avgSize;
            sumSq += diff * diff;
            count++;
        }
        return count == 0 ? 0.0 : Math.sqrt(sumSq / count);
    }

    private double computeBuySellImbalance(List<DetailRow> rows) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        long buys = rows.stream().filter(row -> "BUY".equalsIgnoreCase(row.side)).count();
        long sells = rows.stream().filter(row -> "SELL".equalsIgnoreCase(row.side)).count();
        long total = buys + sells;
        if (total == 0) {
            return 0.0;
        }
        return Math.abs(buys - sells) / (double) total;
    }

    private int countClientTypes(String clientTypes) {
        if (clientTypes == null || clientTypes.isBlank()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(clientTypes.split("\\|"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .distinct()
                .count();
    }

    private QuoteAgeStats computeQuoteAgeStats(List<DetailRow> rows) {
        if (rows.isEmpty()) {
            return new QuoteAgeStats(0.0, 0.0, 0.0);
        }
        List<Long> ages = rows.stream()
                .map(row -> row.quoteAgeMs)
                .sorted(Comparator.naturalOrder())
                .toList();
        double mean = ages.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long[] sorted = new long[ages.size()];
        for (int i = 0; i < ages.size(); i++) {
            sorted[i] = ages.get(i);
        }
        return new QuoteAgeStats(
                mean,
                CommonUtils.percentile(sorted, 0.50),
                CommonUtils.percentile(sorted, 0.90));
    }

    private double scale(double value, double divisor) {
        if (divisor == 0.0) {
            return value;
        }
        return value / divisor;
    }

    private record QuoteAgeStats(double mean, double p50, double p90) {
    }
}
