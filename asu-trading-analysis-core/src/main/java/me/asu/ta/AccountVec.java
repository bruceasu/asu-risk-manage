package me.asu.ta;

import static me.asu.ta.util.CommonUtils.*;
public final class AccountVec {
    public final String accountId;
    public final double[] x;
    public final double norm;

    /** 构造账户向量，并缓存其范数。 */
    public AccountVec(String accountId, double[] x) {
        this.accountId = accountId;
        this.x = x;
        this.norm = me.asu.ta.util.CommonUtils.l2(x);
    }

    /** 从账户聚合统计构建特征向量，并归一化到单位向量。 */
    public static AccountVec from(String acc, Agg a) {
        double n = a.n;
        if (n <= 0)
            return null;
        double avg100 = avg(a.sumMark100, n);
        double avg500 = avg(a.sumMark500, n);
        double avg1s = avg(a.sumMark1s, n);
        double avg5s = avg(a.sumMark5s, n);
        double pos500 = ratio(a.pos500, n);
        double pos1s = ratio(a.pos1s, n);
        double qage = avg(a.sumQuoteAge, n);
        double tpm = tradesPerMin(a);
        double symCount = a.symbols.size();
        double[] v = new double[] {
                avg100, avg500, avg1s, avg5s, pos500, pos1s, qage / 100.0, tpm / 10.0,
                symCount / 10.0
        };
        double norm = l2(v);
        if (norm == 0)
            return null;
        for (int i = 0; i < v.length; i++)
            v[i] /= norm;
        return new AccountVec(acc, v);
    }
}