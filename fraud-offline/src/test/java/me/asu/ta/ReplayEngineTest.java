package me.asu.ta;

import java.nio.file.Files;
import java.nio.file.Path;
import me.asu.ta.offline.ReplayEngine;
import me.asu.ta.offline.ReplayCliOptions;
import me.asu.ta.offline.ReplayState;
import org.junit.Assert;
import org.junit.Test;

public class ReplayEngineTest {
    @Test
    public void replayShouldKeepTradesWithoutQuotesForNonQuoteSignals() throws Exception {
        Path tempDir = Files.createTempDirectory("offline-replay-analysis");
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
                A1,EURUSD,SELL,2000,2.0,2.0,,,
                A2,GBPUSD,BUY,1500,1.0,1.0,,,
                """);

        ReplayCliOptions options = ReplayCliOptions.fromArgs(new String[] {
                "--trades", trades.toString(),
                "--quotes", quotes.toString(),
                "--quoteage-stats",
                "--time-bucket-min", "1"
        });

        ReplayState state = ReplayEngine.replay(options.getTradesPath(), options.getQuotesPath(), options.getReplay());

        Assert.assertEquals(3, state.getDetailRows().size());
        Assert.assertEquals(1, state.getAggByAccount().size());
        Assert.assertEquals(1, state.getAggByAccountSymbol().size());
        Assert.assertEquals(2, state.getAccountTrackers().size());
        Assert.assertFalse(state.getBuckets().isEmpty());
        Assert.assertFalse(state.getQuoteAgeSamples().isEmpty());
        DetailRow missingQuoteRow = state.getDetailRows().stream()
                .filter(row -> "GBPUSD".equals(row.symbol))
                .findFirst()
                .orElseThrow();
        Assert.assertTrue(Double.isNaN(missingQuoteRow.mid0));
        Assert.assertEquals(-1L, missingQuoteRow.lastQuoteT0);
        Assert.assertEquals(-1L, missingQuoteRow.quoteAgeMs);
        Assert.assertNull(missingQuoteRow.marks[0]);
    }
}
