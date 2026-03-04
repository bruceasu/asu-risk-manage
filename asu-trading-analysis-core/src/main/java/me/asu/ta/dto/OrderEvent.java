package me.asu.ta.dto;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 扩展的订单事件（用于实际生产环境）
 * 
 * 使用说明：
 * 1. 从 Binlog 或消息队列监听 OrderExtPo 的变化
 * 2. 解析为 EnhancedOrderEvent
 * 3. 替换 RiskLoop 中的 TradeEv
 * 
 */
public final class OrderEvent implements Event {
    public final long orderId;
    public final int accountId;
    public final int symbolId;
    public final String symbolCode;
    public final byte side;              // 1 = BUY, -1 = SELL
    public final long transactionTime;   // 成交时间
    public final long orderTime;         // 下单时间
    
    // === 价格信息 ===
    public final BigDecimal price;       // 成交价格
    public final BigDecimal bid;         // 市场 bid
    public final BigDecimal ask;         // 市场 ask
    public final BigDecimal orderQty;    // 订单数量
    
    // === 止盈止损 ===
    public final BigDecimal takeProfitPrice;
    public final BigDecimal stopLossPrice;
    
    // === EventText JSON 字段 ===
    public final String eventText;       // 需要解析的 JSON 字符串
    
    // 缓存解析结果
    public EventText parsedEventText;
    
    public long ts() { return transactionTime; }
    
    /**
     * 计算市场中间价
     */
    public BigDecimal getMidPrice() {
        return bid.add(ask).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
    }
    
    /**
     * 构建器入口
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for OrderEvent instances.
     */
    public static final class Builder {
        private long orderId;
        private int accountId;
        private int symbolId;
        private String symbolCode;
        private byte side;
        private long transactionTime;
        private long orderTime;
        private BigDecimal price;
        private BigDecimal bid;
        private BigDecimal ask;
        private BigDecimal orderQty;
        private BigDecimal takeProfitPrice;
        private BigDecimal stopLossPrice;
        private String eventText;
        
        Builder() {
        }
        
        public Builder orderId(long v) { this.orderId = v; return this; }
        public Builder accountId(int v) { this.accountId = v; return this; }
        public Builder symbolId(int v) { this.symbolId = v; return this; }
        public Builder symbolCode(String v) { this.symbolCode = v; return this; }
        public Builder side(byte v) { this.side = v; return this; }
        public Builder transactionTime(long v) { this.transactionTime = v; return this; }
        public Builder orderTime(long v) { this.orderTime = v; return this; }
        public Builder price(BigDecimal v) { this.price = v; return this; }
        public Builder bid(BigDecimal v) { this.bid = v; return this; }
        public Builder ask(BigDecimal v) { this.ask = v; return this; }
        public Builder orderQty(BigDecimal v) { this.orderQty = v; return this; }
        public Builder takeProfitPrice(BigDecimal v) { this.takeProfitPrice = v; return this; }
        public Builder stopLossPrice(BigDecimal v) { this.stopLossPrice = v; return this; }
        public Builder eventText(String v) { this.eventText = v; return this; }
        
        public OrderEvent build() {
            return new OrderEvent(orderId, accountId, symbolId, symbolCode,
                    side, transactionTime, orderTime,
                    price, bid, ask, orderQty,
                    takeProfitPrice, stopLossPrice,
                    eventText);
        }
    }

    /**
     * 构造函数
     */
    public OrderEvent (
            long orderId, int accountId, int symbolId, String symbolCode,
            byte side, long transactionTime, long orderTime,
            BigDecimal price, BigDecimal bid, BigDecimal ask, BigDecimal orderQty,
            BigDecimal takeProfitPrice, BigDecimal stopLossPrice,
            String eventText) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbolId = symbolId;
        this.symbolCode = symbolCode;
        this.side = side;
        this.transactionTime = transactionTime;
        this.orderTime = orderTime;
        this.price = price;
        this.bid = bid;
        this.ask = ask;
        this.orderQty = orderQty;
        this.takeProfitPrice = takeProfitPrice;
        this.stopLossPrice = stopLossPrice;
        this.eventText = eventText;
    }
    
    /**
     * 获取解析后的 EventText（懒加载）
     */
    public EventText getEventText() {
        if (parsedEventText == null) {
            parsedEventText = EventText.parse(eventText);
        }
        return parsedEventText;
    }
    
    /**
     * 计算市场中间价
     */
    public double mid() {
        if (bid == null || ask == null) return 0;
        return bid.add(ask).divide(BigDecimal.valueOf(2)).doubleValue();
    }
    
    /**
     * 从数据库 OrderExtPo 对象转换
     * 注意：这需要您项目中的 OrderExtPo 类定义
     * 
     * 示例代码（需要根据实际类调整）：
     * 
     * public static EnhancedOrderEvent fromOrderExtPo(OrderExtPo po, int symbolId) {
     *     byte side = "BUY".equals(po.getSide().name()) ? (byte)1 : (byte)-1;
     *     
     *     return new EnhancedOrderEvent(
     *         po.getOrderId(),
     *         po.getAccount().intValue(),
     *         symbolId,
     *         po.getSymbolCode(),
     *         side,
     *         po.getTransactionTime().getTime(),
     *         po.getOrderTime().getTime(),
     *         po.getPrice(),
     *         po.getBid(),
     *         po.getAsk(),
     *         po.getOrderQty(),
     *         po.getTakeProfitPrice(),
     *         po.getStopLossPrice(),
     *         po.getEventText()
     *     );
     * }
     */


}