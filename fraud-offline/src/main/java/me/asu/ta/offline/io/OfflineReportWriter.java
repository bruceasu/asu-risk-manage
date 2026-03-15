package me.asu.ta.offline.io;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import me.asu.ta.Anomaly;
import me.asu.ta.BaselineStats;
import me.asu.ta.FxReplayWriter;
import me.asu.ta.risk.model.RiskScoreResult;

public final class OfflineReportWriter {
    public void writeRiskReport(Path out, List<Anomaly> anomalies, int topN, BaselineStats baseline) throws Exception {
        FxReplayWriter.writeReport(
                out,
                anomalies,
                topN,
                baseline.mean500,
                baseline.std500,
                baseline.meanQA,
                baseline.stdQA);
    }

    public void appendIntegratedRiskSummary(Path out, Map<String, RiskScoreResult> riskResults) throws Exception {
        if (riskResults == null || riskResults.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator());
        builder.append("Unified Risk Results").append(System.lineSeparator());
        builder.append("--------------------").append(System.lineSeparator());
        riskResults.values().stream()
                .sorted((left, right) -> Double.compare(right.riskScore(), left.riskScore()))
                .forEach(result -> builder.append(result.accountId())
                        .append(" score=").append(result.riskScore())
                        .append(" level=").append(result.riskLevel())
                        .append(" reasons=").append(result.topReasonCodes())
                        .append(System.lineSeparator()));
        Files.writeString(out, builder.toString(), java.nio.file.StandardOpenOption.APPEND);
    }
}
