package me.asu.ta;

public final class RollingStats {
    private final long[] cnt = new long[RiskConfig.ROLL_WIN_SEC];
    private final double[] sum = new double[RiskConfig.ROLL_WIN_SEC];
    private final double[] sumSq = new double[RiskConfig.ROLL_WIN_SEC];

    private long totalCnt = 0;
    private double totalSum = 0;
    private double totalSumSq = 0;

    private long lastSec = Long.MIN_VALUE;

    public void add(double x, long tsMs) {
        long sec = tsMs / RiskConfig.ROLL_BUCKET_MS;
        rotateTo(sec);

        int idx = (int) (sec % RiskConfig.ROLL_WIN_SEC);
        cnt[idx] += 1;
        sum[idx] += x;
        sumSq[idx] += x * x;

        totalCnt += 1;
        totalSum += x;
        totalSumSq += x * x;
    }


    public long n() {
        return totalCnt;
    }

    public double mean() {
        return totalCnt == 0 ? 0.0 : totalSum / totalCnt;
    }

    public double std() {
        if (totalCnt <= 1) return 0.0;
        double m = mean();
        double var = (totalSumSq / totalCnt) - m * m;
        return var > 0 ? Math.sqrt(var) : 0.0;
    }

    
    private void rotateTo(long sec) {
        if (lastSec == Long.MIN_VALUE) {
            lastSec = sec;
            return;
        }
        long diff = sec - lastSec;
        if (diff <= 0) return;

        long steps = Math.min(diff, RiskConfig.ROLL_WIN_SEC + 1L);
        for (long s = lastSec + 1; s <= lastSec + steps; s++) {
            int idx = (int) (s % RiskConfig.ROLL_WIN_SEC);
            totalCnt -= cnt[idx];
            totalSum -= sum[idx];
            totalSumSq -= sumSq[idx];
            cnt[idx] = 0;
            sum[idx] = 0;
            sumSq[idx] = 0;
        }
        lastSec = sec;
    }
}