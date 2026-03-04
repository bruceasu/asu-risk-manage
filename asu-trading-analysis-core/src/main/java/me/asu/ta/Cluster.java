package me.asu.ta;

import java.util.ArrayList;
import java.util.List;

public final class Cluster {
    public final List<AccountVec> members = new ArrayList<>();
    public final double[] centroid;
    public final String note;

    /** 构造一个簇，包含初始中心和说明文本。 */
    public Cluster(double[] centroid, String note) {
        this.centroid = centroid;
        this.note = note;
    }
}