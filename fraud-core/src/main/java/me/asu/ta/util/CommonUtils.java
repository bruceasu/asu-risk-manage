package me.asu.ta.util;

import java.util.Locale;

import me.asu.ta.Agg;
import me.asu.ta.Side;

public class CommonUtils {
    /**
     * 工具类，不允许实例化。
     */
    private CommonUtils() {}

    /**
     * 计算某个时间戳所属时间桶的起始毫秒。
     */
    public static long bucketStartMs(long t, int bucketMin) {
        long bucketMs = bucketMin * 60_000L;
        return (t / bucketMs) * bucketMs;
    }

    /**
     * 生成时间桶聚合的复合 key，格式：bucketStart|group。
     */
    public static String makeBucketKey(String bucketBy, long bucketStart, String acc, String sym) {
        String g;
        switch (bucketBy) {
        case "account" -> g = "account=" + acc;
        case "symbol" -> g = "symbol=" + sym;
        case "account_symbol" -> g = "account=" + acc + ";symbol=" + sym;
        case "all" -> g = "all";
        default -> g = "all";
        }
        return bucketStart + "|" + g;
    }

    /**
     * 从 bucket key 中解析桶起始时间；解析失败返回 0。
     */
    public static long parseBucketStartFromKey(String key) {
        int p = key.indexOf('|');
        if (p < 0) return 0;
        try {
            return Long.parseLong(key.substring(0, p));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 按 quote-age 统计维度生成 key。
     */
    public static String makeQuoteAgeKey(String scope, String acc, String sym) {
        return switch (scope) {
            case "account" -> "account=" + acc;
            case "symbol" -> "symbol=" + sym;
            case "account_symbol" -> "account=" + acc + ";symbol=" + sym;
            case "all" -> "all";
            default -> "all";
        };
    }

    /**
     * 安全均值：n<=0 返回 0。
     */
    public static double avg(double sum, double n) {
        return n <= 0 ? 0.0 : sum / n;
    }

    /**
     * 安全比例：n<=0 返回 0。
     */
    public static double ratio(long pos, double n) {
        return n <= 0 ? 0.0 : ((double) pos) / n;
    }

    /**
     * 按分组时间跨度计算每分钟交易数。
     */
    public static double tradesPerMin(Agg a) {
        if (a.n <= 0 || a.minT == Long.MAX_VALUE || a.maxT == Long.MIN_VALUE) return 0.0;
        long spanMs = Math.max(1, a.maxT - a.minT);
        double mins = spanMs / 60_000.0;
        return mins <= 0 ? 0.0 : a.n / mins;
    }

    /**
     * 计算向量 L2 范数。
     */
    public static double l2(double[] v) {
        double s = 0;
        for (double x : v) s += x * x;
        return Math.sqrt(s);
    }

    /**
     * 解析交易方向文本为枚举。
     */
    public static Side parseSide(String s) {
        if (s == null) return null;
        String v = s.trim().toUpperCase(Locale.ROOT);
        return switch (v) {
            case "BUY", "B" -> Side.BUY;
            case "SELL", "S" -> Side.SELL;
            default -> null;
        };
    }

    /**
     * 安全解析 double，失败时返回 null。
     */
    public static Double parseDoubleSafe(String s) {
        try {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 安全解析 long，失败时返回 null。
     */
    public static Long parseLongSafe(String s) {
        try {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 宽松解析 int，失败返回 0。
     */
    public static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 宽松解析 double，失败返回 0.0。
     */
    public static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 宽松解析布尔值，支持 true/1/yes/y。
     */
    public static boolean parseBool(String s) {
        s = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y");
    }

    /**
     * double 转 8 位小数；空值或非法数返回空字符串。
     */
    public static String fmt8(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return "";
        return String.format(Locale.ROOT, "%.8f", v);
    }

    /**
     * double 转 10 位小数。
     */
    public static String fmt10(double v) {
        return String.format(Locale.ROOT, "%.10f", v);
    }

    /**
     * double 转 4 位小数。
     */
    public static String fmt4(double v) {
        return fmt(v, 4);
    }

    /**
     * double 转 2 位小数。
     */
    public static String fmt2(double v) {
        return fmt(v, 2);
    }

    /**
     * double 转 6 位小数。
     */
    public static String fmt6(double v) {
        return fmt(v, 6);
    }

    /**
     * double 转 4 位小数（简写）。
     */
    public static String fmt(double v) {
        return fmt(v, 4);
    }

    public static String fmt(double v, int decimals) {
        String fmt = String.format("%%.%df", decimals);
        return String.format(Locale.ROOT, fmt, v);
    }

    /**
     * 计算 z-score；std 为 0 时返回 0 防止除零。
     */
    public static double zscore(double accountMean, double globalMean, double globalStd) {
        if (globalStd == 0) return 0;
        return (accountMean - globalMean) / globalStd;
    }

    /**
     * 计算指定方向的一笔 markout（方向化）；未来价格缺失时返回 null。
     * BUY 看上涨收益，SELL 看下跌收益。
     * 统一处理 null 情况，返回 Double 类型方便在 markout 数组中使用。
     */
    public static Double computeMark(Side s, double m0, Double m1) {
        if (m1 == null) return null;
        return s == Side.BUY ? m1 - m0 : m0 - m1;
    }

    /**
     * 基于 long n 的均值计算。
     */
    public static double mean(double sum, long n) {
        return n == 0 ? 0 : sum / n;
    }

    /**
     * 基于 long n 的标准差计算。
     */
   public static double std(double sum, double sumSq, long n) {
        if (n <= 1) return 0;
        double m = sum / n;
        double variance = (sumSq / n) - m * m;
        return Math.sqrt(Math.max(0.0, variance));
    }

    /**
     * 基于 sum/sumSq/n 计算标准差（double 计数版）。
     */
    public static double std(double sum, double sumSq, double n) {
        if (n <= 1) return 0;
        double mean = sum / n;
        double variance = (sumSq / n) - mean * mean;
        return Math.sqrt(Math.max(0.0, variance));
    }

    public static double sampleStd(double sum, double sumSq, long n) {
        if (n <= 1) return 0;
        double variance = (sumSq - (sum * sum) / n) / (n - 1);
        return Math.sqrt(Math.max(0.0, variance));
    }

    /**
     * 在已排序数组上计算分位数（线性插值）。
     * 算法：把 p 映射到 [0, n-1] 的实数下标 idx，取邻近 lo/hi 两点按比例插值。
     */
    public static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) return 0;
        if (p <= 0) return sorted[0];
        if (p >= 1) return sorted[sorted.length - 1];
        double idx = p * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted[lo];
        double frac = idx - lo;
        return (long) Math.round(sorted[lo] * (1 - frac) + sorted[hi] * frac);
    }
}