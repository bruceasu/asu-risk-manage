package me.asu.ta.offline.io;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import me.asu.ta.offline.analysis.AccountBehaviorFeatureVector;
import me.asu.ta.offline.analysis.BehaviorClusterMember;
import me.asu.ta.offline.analysis.BehaviorSimilarityEdge;
import me.asu.ta.Agg;
import me.asu.ta.BaselineStats;
import me.asu.ta.DetailRow;
import me.asu.ta.FxReplayWriter;
import me.asu.ta.LongSamples;
import me.asu.ta.OfflineAccountTracker;
import com.csvreader.CsvWriter;
import java.nio.charset.StandardCharsets;
import me.asu.ta.util.CommonUtils;

public final class OfflineCsvWriter {
    public void writeDetail(Path out, List<DetailRow> rows) throws Exception {
        FxReplayWriter.writeDetail(out, rows);
    }

    public void writeAggAccountSymbol(Path out, Map<String, Agg> agg) throws Exception {
        FxReplayWriter.writeAggAccountSymbol(out, agg);
    }

    public BaselineStats writeAggAccount(Path out, Map<String, Agg> aggAccount, int minTrades, Agg global) throws Exception {
        return FxReplayWriter.writeAggAccount(out, aggAccount, minTrades, global);
    }

    public void writeBuckets(Path out, int bucketMin, String bucketBy, Map<String, Agg> buckets) throws Exception {
        FxReplayWriter.writeBuckets(out, bucketMin, bucketBy, buckets);
    }

    public void writeQuoteAgeStats(Path out, String scope, Map<String, LongSamples> samples) throws Exception {
        FxReplayWriter.writeQuoteAgeStats(out, scope, samples);
    }

    public void writeBaseline(Path out, BaselineStats baseline) throws Exception {
        FxReplayWriter.writeBaseline(out, baseline);
    }

    public void writeBotIndicators(Path out, Map<String, OfflineAccountTracker> trackers) throws Exception {
        FxReplayWriter.writeBotIndicators(out, trackers);
    }

    public void writeBehaviorFeatures(Path out, Map<String, AccountBehaviorFeatureVector> features) throws Exception {
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[] {
                    "account_id",
                    "trade_count",
                    "symbol_count",
                    "avg_holding_time_proxy_ms",
                    "quote_age_mean_ms",
                    "quote_age_p50_ms",
                    "quote_age_p90_ms",
                    "inter_arrival_cv",
                    "entropy",
                    "tpsl_ratio",
                    "client_ip_count",
                    "client_type_count",
                    "avg_size",
                    "size_std_like",
                    "buy_sell_imbalance",
                    "markout500_mean",
                    "markout1s_mean",
                    "vector_norm"
            });
            for (AccountBehaviorFeatureVector feature : features.values()) {
                writer.writeRecord(new String[] {
                        feature.getAccountId(),
                        Long.toString(feature.getTradeCount()),
                        Integer.toString(feature.getSymbolCount()),
                        CommonUtils.fmt4(feature.getAvgHoldingTimeProxy()),
                        CommonUtils.fmt4(feature.getQuoteAgeMean()),
                        CommonUtils.fmt4(feature.getQuoteAgeP50()),
                        CommonUtils.fmt4(feature.getQuoteAgeP90()),
                        CommonUtils.fmt4(feature.getInterArrivalCv()),
                        CommonUtils.fmt4(feature.getEntropy()),
                        CommonUtils.fmt4(feature.getTpslRatio()),
                        Integer.toString(feature.getClientIpCount()),
                        Integer.toString(feature.getClientTypeCount()),
                        CommonUtils.fmt4(feature.getAvgSize()),
                        CommonUtils.fmt4(feature.getSizeStdLike()),
                        CommonUtils.fmt4(feature.getBuySellImbalance()),
                        CommonUtils.fmt10(feature.getMarkout500Mean()),
                        CommonUtils.fmt10(feature.getMarkout1sMean()),
                        CommonUtils.fmt4(feature.getRawVectorNorm())
                });
            }
        }
    }

    public void writeBehaviorClusters(Path out, List<BehaviorClusterMember> clusters) throws Exception {
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[] {
                    "cluster_id",
                    "account_id",
                    "cluster_size",
                    "vector_norm",
                    "note"
            });
            for (BehaviorClusterMember member : clusters) {
                writer.writeRecord(new String[] {
                        Integer.toString(member.clusterId()),
                        member.accountId(),
                        Integer.toString(member.clusterSize()),
                        CommonUtils.fmt4(member.vectorNorm()),
                        member.note()
                });
            }
        }
    }

    public void writeSimilarityEdges(Path out, List<BehaviorSimilarityEdge> edges) throws Exception {
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[] {
                    "left_account_id",
                    "right_account_id",
                    "similarity",
                    "left_rank",
                    "right_rank",
                    "note"
            });
            for (BehaviorSimilarityEdge edge : edges) {
                writer.writeRecord(new String[] {
                        edge.leftAccountId(),
                        edge.rightAccountId(),
                        CommonUtils.fmt4(edge.similarity()),
                        Integer.toString(edge.leftRank()),
                        Integer.toString(edge.rightRank()),
                        edge.note()
                });
            }
        }
    }
}
