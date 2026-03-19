package me.asu.ta;

import com.csvreader.CsvWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * <h1>账户相似性聚类算法</h1>
 *
 * 基于账户的多维风险指标（markout、quote age、交易频率等）构建特征向量，
 * 然后使用 k-means 或阈值聚类算法快速定位"高度相似的账户组"。
 *
 * <h2>📊 特征向量构成</h2>
 *
 * 每个账户的特征向量包含 5 维数据（见 {@link AccountVec}）：
 *
 * <ul>
 *   <li><b>avg_markout_500ms</b> — 500ms markout 平均值，反映执行质量</li>
 *   <li><b>pos_ratio_500ms</b> — 500ms 正收益比例，反映逆向选择程度</li>
 *   <li><b>avg_quote_age</b> — 平均报价时效，反映系统响应能力</li>
 *   <li><b>trades_per_min</b> — 每分钟交易数，反映交易活跃度</li>
 *   <li><b>symbol_count</b> — 交易品种数，反映多元化程度</li>
 * </ul>
 *
 * 向量在构建后会进行 L2 归一化，便于使用余弦相似度进行聚类。
 *
 * <h2>🔀 聚类算法</h2>
 *
 * 工具支持两种聚类方式（通过参数 `--cluster-k` 控制）：
 *
 * <h3>1. 阈值聚类（贪心算法）</h3>
 *
 * **启用条件**：`--cluster-k 0`（默认）
 *
 * **算法原理**：
 * <pre>
 * 1. 初始化空簇列表
 * 2. 遍历每个账户向量 v：
 *    a. 计算 v 与所有簇中心的余弦相似度
 *    b. 如果最大相似度 >= threshold（默认 0.92）：
 *       - 并入最相似簇
 *       - 重算簇中心
 *    c. 否则创建新簇
 * 3. 按簇大小降序排序
 * </pre>
 *
 * **优点**：
 * - 无需预先指定簇数
 * - 通过 threshold 参数灵活控制相似度阈值
 * - 适合探索性分析
 *
 * **缺点**：
 * - 贪心策略可能不是全局最优
 * - 顺序敏感（不同顺序可能产生不同簇）
 *
 * <h3>2. K-means 聚类（简化版）</h3>
 *
 * **启用条件**：`--cluster-k <K>`（K > 0）
 *
 * **算法原理**：
 * <pre>
 * 1. 初始化前 K 个样本的特征向量作为簇中心
 * 2. 执行 30 次迭代：
 *    a. 清空所有簇的成员
 *    b. 遍历每个向量，分配到最相似的簇
 *    c. 重算所有簇的中心向量
 * 3. 按簇大小降序排序
 * </pre>
 *
 * **优点**：
 * - 簇数固定，结果可重复
 * - 较快收敛
 * - 适合已知账户分类数量的场景
 *
 * **缺点**：
 * - 需要提前指定簇数 K
 * - 初始中心选择影响结果
 *
 * <h2>📏 相似度度量</h2>
 *
 * 使用 **余弦相似度**：
 * <pre>
 * cosine(a, b) = Σ(a[i] * b[i]) / (||a|| * ||b||)
 * </pre>
 *
 * 由于向量已归一化，简化为点积。相似度范围 [0, 1]。
 *
 * <h2>📤 输出格式</h2>
 *
 * 输出 CSV 文件包含 4 列：
 *
 * <ul>
 *   <li><b>cluster_id</b> — 簇编号（从 1 开始），按簇大小降序</li>
 *   <li><b>account_id</b> — 账户 ID</li>
 *   <li><b>vector_norm</b> — 向量 L2 范数（特征值大小）</li>
 *   <li><b>note</b> — 簇说明（算法及参数）</li>
 * </ul>
 *
 * **示例**：
 * <pre>
 * cluster_id,account_id,vector_norm,note
 * 1,ACC001,1.2345,threshold=0.92
 * 1,ACC002,1.1234,threshold=0.92
 * 2,ACC003,0.9876,threshold=0.92
 * </pre>
 *
 * <h2>⚙️ 使用建议</h2>
 *
 * <h3>探索性分析</h3>
 * <pre>{@code
 * --cluster true --cluster-k 0 --cluster-threshold 0.92
 * }</pre>
 * 让算法自动决定簇数，适合首次分析
 *
 * <h3>已知分类数</h3>
 * <pre>{@code
 * --cluster true --cluster-k 5
 * }</pre>
 * 固定 5 个簇，适合已有行业分类的场景
 *
 * <h3>调整严格度</h3>
 * <pre>{@code
 * --cluster true --cluster-threshold 0.95  # 更严格
 * --cluster true --cluster-threshold 0.85  # 更宽松
 * }</pre>
 * 提高阈值得到更多小簇，降低阈值得到更少大簇
 *
 * <h2>🔍 解读结果</h2>
 *
 * <b>优先关注</b>：
 * <ol>
 *   <li><b>簇 1（最大）</b> — 包含相似账户最多的组，可能是主流策略或执行模式</li>
 *   <li><b>小簇（< 5 个账户）</b> — 异常账户或小众策略，需要人工核查</li>
 *   <li><b>vector_norm</b> — 值大表示特征突出，值小表示特征平凡</li>
 * </ol>
 *
 * <b>典型模式</b>：
 * <pre>
 * 簇 1: 50 个账户 (norm ≈ 1.0)  → 正常交易群体
 * 簇 2: 8 个账户  (norm ≈ 2.5)  → 高标记账户（异常模式）
 * 簇 3: 3 个账户  (norm ≈ 0.3)  → 低活跃账户（边缘用户）
 * </pre>
 *
 * <h2>⚠️ 注意事项</h2>
 *
 * - **数据预处理**：只有交易数 >= `--min-trades` 的账户才会参与聚类
 * - **向量计算失败**：某些账户如果数据不完整，可能无法构建向量（被过滤）
 * - **初始化敏感**：k-means 结果可能因初始中心不同而变化，建议尝试多个 K 值
 * - **相似度饱和**：高度相似账户的余弦相似度可能达到 0.95+ 甚至更高
 *
 * @see AccountVec 账户特征向量
 * @see Cluster 聚类簇
 */
public final class ClusterHelper {
    /** 工具类，不允许实例化。 */
    private ClusterHelper() {}

    /**
     * 构建账户特征向量并执行聚类，最终将结果写出 CSV 文件。
     *
     * <p><b>执行流程</b>：</p>
     * <ol>
     *   <li>从账户聚合数据（{@link Agg}）构建特征向量（{@link AccountVec}）</li>
     *   <li>过滤交易数 < minTrades 的账户</li>
     *   <li>选择聚类算法（k-means 或阈值聚类）</li>
     *   <li>生成聚类结果</li>
     *   <li>写出 CSV 文件（使用 {@link CsvWriter}）</li>
     * </ol>
     *
     * @param out 输出 CSV 文件路径
     * @param aggByAccount 账户聚合统计数据（Map: account -> Agg）
     * @param k k-means 簇数（> 0 启用 k-means，<= 0 使用阈值聚类）
     * @param threshold 阈值聚类的相似度阈值（默认 0.92），仅在 k <= 0 时使用
     * @param minTrades 最小交易数过滤门槛，低于此值的账户不参与聚类
     * @throws IOException 如果文件写入失败
     * @see #kMeans(List, int, int)
     * @see #thresholdClustering(List, double)
     */
    public static void clusterAccountsAndWrite(Path out, Map<String, Agg> aggByAccount, int k,
            double threshold, int minTrades) throws IOException {
        List<AccountVec> vecs = aggByAccount.entrySet().stream()
                .filter(e -> e.getValue().n >= minTrades)
                .map(e -> AccountVec.from(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .toList();

        List<Cluster> clusters = k > 0 ? kMeans(vecs, k, 30) : thresholdClustering(vecs, threshold);
        try (CsvWriter writer = new CsvWriter(out.toString(), ',', StandardCharsets.UTF_8)) {
            writer.writeRecord(new String[]{"cluster_id", "account_id", "vector_norm", "note"});
            int cid = 0;
            for (Cluster c : clusters) {
                cid++;
                for (AccountVec v : c.members) {
                    writer.writeRecord(new String[]{
                            Integer.toString(cid), v.accountId, me.asu.ta.util.CommonUtils.fmt4(v.norm), c.note});
                }
            }
        }
    }

    /**
     * 阈值聚类（贪心算法）。
     *
     * <p><b>算法</b>：</p>
     * 遍历每个向量，若与现有某簇中心的余弦相似度 >= threshold，则并入最相似簇；
     * 否则新建簇。并入簇后重算中心向量（成员均值 + L2 归一化）。
     *
     * <p><b>复杂度</b>：O(n * m * d)，其中 n=向量数，m=簇数，d=维数</p>
     *
     * @param vecs 已归一化的特征向量列表
     * @param threshold 相似度阈值（0-1），默认 0.92
     * @return 聚类簇列表，按簇大小降序排序
     * @see #cosine(double[], double[])
     * @see #recomputeCentroid(Cluster)
     */
    public static List<Cluster> thresholdClustering(List<AccountVec> vecs, double threshold) {
        List<Cluster> clusters = new ArrayList<>();
        for (AccountVec v : vecs) {
            Cluster best = null;
            double bestSim = -1;
            for (Cluster c : clusters) {
                double sim = cosine(v.x, c.centroid);
                if (sim >= threshold && sim > bestSim) {
                    best = c;
                    bestSim = sim;
                }
            }
            if (best == null) {
                Cluster c = new Cluster(Arrays.copyOf(v.x, v.x.length), "threshold=" + threshold);
                c.members.add(v);
                clusters.add(c);
            } else {
                best.members.add(v);
                recomputeCentroid(best);
            }
        }
        clusters.sort((a, b) -> Integer.compare(b.members.size(), a.members.size()));
        return clusters;
    }

    /**
     * 以簇的当前成员均值重算簇中心，并进行 L2 归一化。
     *
     * <p><b>步骤</b>：</p>
     * <ol>
     *   <li>清零中心向量</li>
     *   <li>累加所有成员向量</li>
     *   <li>求均值</li>
     *   <li>L2 归一化（除以范数）</li>
     * </ol>
     *
     * @param c 待重算的簇（会直接修改其 centroid 字段）
     * @see me.asu.ta.util.CommonUtils#l2(double[])
     */
    public static void recomputeCentroid(Cluster c) {
        Arrays.fill(c.centroid, 0);
        for (AccountVec v : c.members) {
            for (int i = 0; i < c.centroid.length; i++) c.centroid[i] += v.x[i];
        }
        for (int i = 0; i < c.centroid.length; i++) c.centroid[i] /= c.members.size();
        double n = me.asu.ta.util.CommonUtils.l2(c.centroid);
        if (n > 0) {
            for (int i = 0; i < c.centroid.length; i++) c.centroid[i] /= n;
        }
    }

    /**
     * 简化 k-means 聚类（使用余弦相似度替代欧氏距离）。
     *
     * <p><b>算法</b>：</p>
     * <ol>
     *   <li>初始化前 k 个样本的特征向量作为簇中心</li>
     *   <li>执行 iters 次迭代：
     *     <ul>
     *       <li>清空所有簇的成员</li>
     *       <li>遍历每个向量，分配到最相似（余弦相似度最大）的簇</li>
     *       <li>重算所有簇的中心向量</li>
     *     </ul>
     *   </li>
     *   <li>按簇大小降序排序</li>
     * </ol>
     *
     * <p><b>复杂度</b>：O(iters * n * k * d)，其中 n=向量数，k=簇数，d=维数</p>
     *
     * @param vecs 已归一化的特征向量列表
     * @param k 簇数（如果 > vecs.size()，自动裁剪）
     * @param iters 迭代次数（默认 30）
     * @return 聚类簇列表，按簇大小降序排序
     * @see #cosine(double[], double[])
     * @see #recomputeCentroid(Cluster)
     */
    public static List<Cluster> kMeans(List<AccountVec> vecs, int k, int iters) {
        if (vecs.isEmpty()) return List.of();
        k = Math.min(k, vecs.size());
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new Cluster(Arrays.copyOf(vecs.get(i).x, vecs.get(i).x.length), "kmeans(k=" + k + ")"));
        }
        for (int it = 0; it < iters; it++) {
            for (Cluster c : clusters) c.members.clear();
            for (AccountVec v : vecs) {
                int best = 0;
                double bestSim = -1;
                for (int i = 0; i < clusters.size(); i++) {
                    double sim = cosine(v.x, clusters.get(i).centroid);
                    if (sim > bestSim) {
                        bestSim = sim;
                        best = i;
                    }
                }
                clusters.get(best).members.add(v);
            }
            for (Cluster c : clusters) {
                if (!c.members.isEmpty()) recomputeCentroid(c);
            }
        }
        clusters.sort((a, b) -> Integer.compare(b.members.size(), a.members.size()));
        return clusters;
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * <p><b>公式</b>：</p>
     * <pre>
     * cosine(a, b) = Σ(a[i] * b[i])
     * </pre>
     *
     * <p>由于向量已归一化，无需再除以范数。相似度范围 [0, 1]。</p>
     *
     * @param a 第一个向量（已归一化）
     * @param b 第二个向量（已归一化）
     * @return 余弦相似度（点积），范围 [0, 1]
     */
    public static double cosine(double[] a, double[] b) {
        double s = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) s += a[i] * b[i];
        return s;
    }
}
