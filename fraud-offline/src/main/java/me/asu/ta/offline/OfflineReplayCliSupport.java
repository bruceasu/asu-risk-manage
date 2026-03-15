package me.asu.ta.offline;

public final class OfflineReplayCliSupport {
    private OfflineReplayCliSupport() {}

    public static boolean hasHelpArg(String[] args) {
        for (String arg : args) {
            if ("-h".equals(arg) || "--help".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static void printHelp() {
        System.out.println("""
                FxReplayPlus - Offline FX Replay Tool

                Usage:
                  java -jar bin/app.jar --trades <file> --quotes <file> [options]
                  java -cp classes FxReplayPlus --trades <file> --quotes <file> [options]

                Required:
                  --trades <file>              trades CSV
                  --quotes <file>              quotes CSV

                Output:
                  --out-dir <dir>              apply default output names under one directory
                  --out-detail <file>          default: markout_detail.csv
                  --out-agg <file>             default: markout_agg_by_account_symbol.csv
                  --out-agg-account <file>     default: markout_agg_by_account.csv
                  --out-bucket <file>          default: markout_time_buckets.csv
                  --out-quoteage <file>        default: quote_age_stats.csv
                  --out-baseline <file>        default: baseline.csv
                  --out-cluster <file>         default: clusters.csv
                  --out-report <file>          default: risk_report.txt
                  --out-chart <file>           default: fx_replay_dashboard.html
                  --out-bot-indicators <file>  default: bot_indicators.csv

                Feature switches:
                  --agg-account                default: off
                  --min-trades <N>             default: 0
                  --time-bucket-min <N>        default: 0 (off)
                  --bucket-by all|account|symbol|account_symbol   default: all
                  --quoteage-stats             default: off
                  --quoteage-scope all|account|symbol|account_symbol  default: all
                  --quoteage-max-samples <N>   default: 200000
                  --cluster                    default: off
                  --cluster-k <K>              default: 0 (threshold mode)
                  --cluster-threshold <T>      default: 0.92
                  --baseline                   default: off
                  --report                     default: off
                  --top-n <N>                  default: 20
                  --charts                     default: off
                  --chart-top-n <N>            default: 20
                  --integrate-current-system   default: off
                """);
    }
}
