package me.asu.ta;

import java.nio.file.Path;

final class OutputOptions {
    final Path detail;
    final Path aggAccountSymbol;
    final Path aggAccount;
    final Path bucket;
    final Path quoteAge;
    final Path baseline;
    final Path cluster;
    final Path report;
    final Path chart;
    final Path botIndicators;

    OutputOptions(
            Path detail,
            Path aggAccountSymbol,
            Path aggAccount,
            Path bucket,
            Path quoteAge,
            Path baseline,
            Path cluster,
            Path report,
            Path chart,
            Path botIndicators) {
        this.detail = detail;
        this.aggAccountSymbol = aggAccountSymbol;
        this.aggAccount = aggAccount;
        this.bucket = bucket;
        this.quoteAge = quoteAge;
        this.baseline = baseline;
        this.cluster = cluster;
        this.report = report;
        this.chart = chart;
        this.botIndicators = botIndicators;
    }
}
