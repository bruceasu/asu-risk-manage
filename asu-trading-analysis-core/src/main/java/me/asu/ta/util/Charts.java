package me.asu.ta.util;

import me.asu.ta.Agg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 生成回放结果的 HTML 仪表盘，包含：
 * - 按 avg_markout_500ms 排名前 N 的账户条形图
 * - 全部账户的 avg_quote_age_ms vs avg_markout_500ms 散点图
 * - 前 N 的账户数据表格
 */
public class Charts {
    private Charts() {}

    private record AccountPoint(
            String accountId,
            long n,
            double avg500,
            double avg1s,
            double avgQuoteAge,
            double pos500,
            double pos1s) {}

    public static void writeDashboard(Path out, Map<String, Agg> aggByAccount, int minTrades, int topN,
            Path tradesPath, Path quotesPath) throws IOException {
        List<AccountPoint> points = buildPoints(aggByAccount, minTrades);
        points.sort((a, b) -> Double.compare(b.avg500, a.avg500));
        int take = Math.min(Math.max(1, topN), points.size());
        List<AccountPoint> top = points.subList(0, take);

        String html = buildHtml(top, points, tradesPath, quotesPath, minTrades);
        Files.writeString(out, html, StandardCharsets.UTF_8);
    }

    private static List<AccountPoint> buildPoints(Map<String, Agg> aggByAccount, int minTrades) {
        List<AccountPoint> out = new ArrayList<>();
        for (var e : aggByAccount.entrySet()) {
            Agg a = e.getValue();
            if (a.n < minTrades) continue;
            double n = a.n;
            out.add(new AccountPoint(
                    e.getKey(),
                    a.n,
                    me.asu.ta.util.CommonUtils.avg(a.sumMark500, n),
                    me.asu.ta.util.CommonUtils.avg(a.sumMark1s, n),
                    me.asu.ta.util.CommonUtils.avg(a.sumQuoteAge, n),
                    me.asu.ta.util.CommonUtils.ratio(a.pos500, n),
                    me.asu.ta.util.CommonUtils.ratio(a.pos1s, n)));
        }
        return out;
    }

    private static String buildHtml(List<AccountPoint> top, List<AccountPoint> all,
            Path tradesPath, Path quotesPath, int minTrades) {
        StringBuilder sb = new StringBuilder(64_000);
        sb.append("""
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>FX Replay Dashboard</title>
                  <style>
                    :root { --bg:#f6f8fb; --fg:#1f2937; --card:#ffffff; --line:#e5e7eb; --green:#16a34a; --red:#dc2626; --blue:#2563eb; }
                    body { margin:0; padding:20px; font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; background:var(--bg); color:var(--fg); }
                    h1,h2 { margin:0 0 12px 0; }
                    .meta { color:#4b5563; margin-bottom:16px; }
                    .grid { display:grid; grid-template-columns:1fr; gap:16px; }
                    .card { background:var(--card); border:1px solid var(--line); border-radius:10px; padding:14px; }
                    table { width:100%; border-collapse:collapse; font-size:13px; }
                    th,td { border-bottom:1px solid var(--line); padding:6px 8px; text-align:right; }
                    th:first-child, td:first-child { text-align:left; }
                    .bar-wrap { background:#eef2ff; height:12px; border-radius:6px; position:relative; }
                    .bar { height:12px; border-radius:6px; }
                    .row-flex { display:grid; grid-template-columns:180px 1fr 90px; gap:8px; align-items:center; margin:6px 0; font-size:13px; }
                    svg { width:100%; height:360px; border:1px solid var(--line); border-radius:8px; background:#fff; }
                    .muted { color:#6b7280; font-size:12px; }
                  </style>
                </head>
                <body>
                """);

        sb.append("<h1>FX Replay Dashboard</h1>\n");
        sb.append("<div class='meta'>")
                .append("trades=").append(escape(tradesPath.toString()))
                .append(" | quotes=").append(escape(quotesPath.toString()))
                .append(" | accounts=").append(all.size())
                .append(" | min_trades=").append(minTrades)
                .append("</div>\n");

        sb.append("<div class='grid'>\n");
        sb.append("<div class='card'><h2>Top Accounts by avg_markout_500ms</h2>\n");
        sb.append(renderTopBars(top));
        sb.append("<div class='muted'>绿色为正，红色为负；条长按绝对值归一化。</div></div>\n");

        sb.append("<div class='card'><h2>Scatter: avg_quote_age_ms vs avg_markout_500ms</h2>\n");
        sb.append(renderScatter(all));
        sb.append("<div class='muted'>X 轴=avg_quote_age_ms，Y 轴=avg_markout_500ms（上正下负）。</div></div>\n");

        sb.append("<div class='card'><h2>Top Accounts Table</h2>\n");
        sb.append(renderTable(top));
        sb.append("</div>\n");
        sb.append("</div>\n</body></html>");
        return sb.toString();
    }

    private static String renderTopBars(List<AccountPoint> top) {
        StringBuilder sb = new StringBuilder();
        double maxAbs = 1e-12;
        for (AccountPoint p : top) maxAbs = Math.max(maxAbs, Math.abs(p.avg500));
        for (AccountPoint p : top) {
            double pct = Math.min(100.0, Math.abs(p.avg500) / maxAbs * 100.0);
            String color = p.avg500 >= 0 ? "var(--green)" : "var(--red)";
            sb.append("<div class='row-flex'>")
                    .append("<div>").append(escape(p.accountId)).append(" (n=").append(p.n).append(")</div>")
                    .append("<div class='bar-wrap'><div class='bar' style='width:")
                    .append(String.format(Locale.ROOT, "%.2f", pct))
                    .append("%;background:").append(color).append("'></div></div>")
                    .append("<div>").append(String.format(Locale.ROOT, "%.8f", p.avg500)).append("</div>")
                    .append("</div>\n");
        }
        return sb.toString();
    }

    private static String renderScatter(List<AccountPoint> all) {
        if (all.isEmpty()) return "<div>no data</div>";
        double maxX = 1e-9;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (AccountPoint p : all) {
            maxX = Math.max(maxX, p.avgQuoteAge);
            minY = Math.min(minY, p.avg500);
            maxY = Math.max(maxY, p.avg500);
        }
        if (minY == maxY) {
            minY -= 1e-6;
            maxY += 1e-6;
        }
        int w = 1000;
        int h = 360;
        int padL = 50, padR = 20, padT = 20, padB = 35;
        double plotW = w - padL - padR;
        double plotH = h - padT - padB;
        double yZero = padT + (maxY / (maxY - minY)) * plotH;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg viewBox='0 0 ").append(w).append(" ").append(h).append("'>");
        sb.append("<line x1='").append(padL).append("' y1='").append(yZero).append("' x2='")
                .append(w - padR).append("' y2='").append(yZero).append("' stroke='#9ca3af' stroke-width='1'/>");
        sb.append("<line x1='").append(padL).append("' y1='").append(padT).append("' x2='")
                .append(padL).append("' y2='").append(h - padB).append("' stroke='#9ca3af' stroke-width='1'/>");

        for (AccountPoint p : all) {
            double x = padL + (p.avgQuoteAge / maxX) * plotW;
            double y = padT + ((maxY - p.avg500) / (maxY - minY)) * plotH;
            String fill = p.avg500 >= 0 ? "#16a34a" : "#dc2626";
            sb.append("<circle cx='").append(fmt(x)).append("' cy='").append(fmt(y))
                    .append("' r='3' fill='").append(fill).append("'>")
                    .append("<title>").append(escape(p.accountId))
                    .append(" | n=").append(p.n)
                    .append(" | avg500=").append(String.format(Locale.ROOT, "%.8f", p.avg500))
                    .append(" | avgQA=").append(String.format(Locale.ROOT, "%.2f", p.avgQuoteAge))
                    .append("</title></circle>");
        }
        sb.append("</svg>");
        return sb.toString();
    }

    private static String renderTable(List<AccountPoint> top) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr>")
                .append("<th>account</th><th>n</th><th>avg500</th><th>avg1s</th><th>pos500</th><th>pos1s</th><th>avg_quote_age_ms</th>")
                .append("</tr></thead><tbody>");
        for (AccountPoint p : top) {
            sb.append("<tr><td>").append(escape(p.accountId)).append("</td>")
                    .append("<td>").append(p.n).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.8f", p.avg500)).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.8f", p.avg1s)).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.4f", p.pos500)).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.4f", p.pos1s)).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.2f", p.avgQuoteAge)).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}