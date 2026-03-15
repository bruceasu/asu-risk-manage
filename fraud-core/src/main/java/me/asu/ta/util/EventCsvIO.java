package me.asu.ta.util;
import me.asu.ta.dto.Event;
import me.asu.ta.dto.OrderEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EventCsvIO {
    private EventCsvIO() {}

    public static List<Event> loadTradeEvents(Path eventsFile) throws IOException {
        List<Event> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(eventsFile, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null)
                throw new IllegalArgumentException("empty events file");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                if (line.startsWith("#"))
                    continue; // 支持注释行
                out.add(parseTrade(line));
            }
        }
        return out;
    }

    /**
     * CSV 格式示例（需要添加新列）：
     * 
     * timestamp,account_id,symbol_id,side,exec_price,event_text,order_size,take_profit,stop_loss
     * 1708444800000,12345,1,B,1.0850,"{\"px\":1.0851,\"tm\":1708444799500,...}",1.5,1.0950,1.0750
     * 1708444801000,23456,1,S,1.0852,"{\"px\":1.0852,\"tm\":1708444800500,...}",2.0,1.0752,1.0952
     * 
     * 解析代码：
     */
    public static OrderEvent parseTrade(String line) {
        String[] parts = line.split(",");

        long ts = Long.parseLong(parts[0]);
        int accountId = Integer.parseInt(parts[1]);
        int symbolId = Integer.parseInt(parts[2]);
        byte side = parts[3].equals("B") ? (byte) 1 : (byte) -1;
        BigDecimal execPrice = new BigDecimal(parts[4]);

        // ===== 新增字段解析 =====
        String eventText = parts.length > 5 ? parts[5] : null;
        BigDecimal orderSize = parts.length > 6 ? new BigDecimal(parts[6]) : BigDecimal.ZERO;
        BigDecimal tp = (parts.length > 7 && !parts[7].isEmpty()) ? new BigDecimal(parts[7]) : BigDecimal.ZERO;
        BigDecimal sl = (parts.length > 8 && !parts[8].isEmpty()) ? new BigDecimal(parts[8]) : BigDecimal.ZERO;

        // 构造 TradeEv 对象，使用builder
        return OrderEvent.builder()
                .accountId(accountId)
                .symbolId(symbolId)
                .transactionTime(ts)
                .side(side)
                .price(execPrice)
                .eventText(eventText)
                .orderQty(orderSize)
                .takeProfitPrice(tp)
                .stopLossPrice(sl)
                .build();
    }
}