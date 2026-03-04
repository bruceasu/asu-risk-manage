package me.asu.ta;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

// TODO：用于写入风险快照数据到 CSV 文件，供后续分析使用。字段要跟 SnapshotDbWroter 一致。
public final class SnapshotFileWriter implements AutoCloseable {
    private final BufferedWriter bw;

    public SnapshotFileWriter(Path out) throws IOException {
        this.bw = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        bw.write("ts_ms,account_id,level,score,z500,z1s,zqa,extra_delay_ms,last_look,max_ops_per_second,n500");
        bw.newLine();
        bw.flush();
    }

    public void write(long tsMs, int accountId, byte level, double score, double z500, double z1s, double zqa,
            ExecutionPolicy p, long n500) throws IOException {
        bw.write(tsMs + "," + accountId + "," + level + ","
                + fmt(score) + "," + fmt(z500) + "," + fmt(z1s) + "," + fmt(zqa) + ","
                + p.extraDelayMs() + "," + p.lastLook() + "," + p.maxOpsPerSecond() + "," + n500);
        bw.newLine();
    }

    public void flush() throws IOException {
        bw.flush();
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.6f", v);
    }
}