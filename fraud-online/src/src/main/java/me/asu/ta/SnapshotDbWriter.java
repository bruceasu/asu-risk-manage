package me.asu.ta;
// 输出增强特征到数据库/CSV

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * 快照写入器
 * 定期将 AccountState 中的增强特征写入数据库或 CSV 文件
 * 用于离线分析和模型训练。
 * 
 * TODO: 完善字段列表。
 */
public class SnapshotDbWriter {
    private final DataSource dataSource;
    public SnapshotDbWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public void writeSnapshot(long nowMs, AccountState[] accounts) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO account_features_enhanced " +
                    "(account_id, window_start_ms, window_end_ms, " +
                    "cv_delta, identical_tpsl_ratio, order_size_entropy, " +
                    "...) VALUES (?, ?, ?, ...)";

            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < accounts.length; i++) {
                AccountState acc = accounts[i];
                if (acc.totalOrders < 10)
                    continue;

                IntervalStats iv = acc.getIntervalStats();

                ps.setInt(1, i);
                ps.setLong(2, nowMs - 60000);
                ps.setLong(3, nowMs);
                ps.setDouble(4, iv.cv());
                ps.setDouble(5, acc.tpslPattern.identicalTPSLRatio());
                ps.setDouble(6, acc.sizeAnalyzer.entropy());
                // ... 更多字段

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}