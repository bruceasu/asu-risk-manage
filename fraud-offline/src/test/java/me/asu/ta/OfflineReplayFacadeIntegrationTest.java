package me.asu.ta;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class OfflineReplayFacadeIntegrationTest {
    @Test
    public void legacyEntryPointShouldDelegateToNewFacadeAndProduceOutputs() throws Exception {
        Path tempDir = Files.createTempDirectory("offline-facade");
        Path outputDir = tempDir.resolve("out");
        Files.createDirectories(outputDir);
        Path quotes = tempDir.resolve("quotes.csv");
        Path trades = tempDir.resolve("trades.csv");

        Files.writeString(quotes, """
                symbol,quote_time_ms,bid,ask
                EURUSD,900,1.0998,1.1002
                EURUSD,1000,1.1000,1.1004
                EURUSD,1100,1.1002,1.1006
                EURUSD,1500,1.1003,1.1007
                EURUSD,2000,1.1004,1.1008
                EURUSD,2500,1.1005,1.1009
                EURUSD,7000,1.1008,1.1012
                """);
        Files.writeString(trades, """
                account_id,symbol,side,exec_time_ms,size,orderSize,takeProfit,stopLoss,eventText
                A1,EURUSD,BUY,1000,1.0,1.0,,,
                A1,EURUSD,SELL,2000,1.0,1.0,,,
                A2,EURUSD,BUY,1100,1.0,1.0,,,
                A2,EURUSD,SELL,2100,1.0,1.0,,,
                """);

        FxReplayPlus.main(new String[] {
                "--trades", trades.toString(),
                "--quotes", quotes.toString(),
                "--out-dir", outputDir.toString(),
                "--agg-account",
                "--baseline",
                "--report",
                "--cluster",
                "--behavior-cluster",
                "--similarity-edges",
                "--min-trades", "1"
        });

        Assert.assertTrue(Files.exists(outputDir.resolve("markout_detail.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("markout_agg_by_account_symbol.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("markout_agg_by_account.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("baseline.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("clusters.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("risk_report.txt")));
        Assert.assertTrue(Files.exists(outputDir.resolve("bot_indicators.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("account_behavior_features.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("account_behavior_clusters.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("account_similarity_edges.csv")));
        Assert.assertTrue(Files.exists(outputDir.resolve("behavior_cluster_report.txt")));
    }
}
