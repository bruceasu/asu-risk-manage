package me.asu.ta.dto;

import java.math.BigDecimal;

/**
 * EventText JSON 字段解析类
 * 对应订单表中的 event_text 字段
 * 
 * JSON 格式示例：
 * {
 *   "px": "109.424",    s    // 客户端请求价格
 *   "tm": 1623227127960,    // 客户端请求时间戳（ms）
 *   "td": -508,             // 时间差 = 服务器时间 - 客户端时间（ms）
 *   "ip": "124.217.137.1",  // 客户端 IP
 *   "clt": "2",             // 客户端类型（1:iOS, 2:Android, 3:Web）
 *   "clv": "1.2.6",         // 客户端版本
 *   "log": "janson-l3",     // 登录名
 *   "cm": "",               // 备注/Comment
 *   "mg": 50000,            // 保证金
 *   "eqt": 55000,           // 净值
 *   "mgl": 110              // 保证金水平（%）
 * }
 */
public final class EventText {
    public final BigDecimal requestPrice;      // px
    public final Long requestTime;             // tm
    public final Long timeDiff;                // td
    public final String ip;                    // ip
    public final String clientType;            // clt
    public final String clientVersion;         // clv
    public final String loginName;             // log
    public final String comment;               // cm
    public final Long margin;                  // mg
    public final Long equity;                  // eqt
    public final Integer marginLevel;          // mgl
    
    public EventText(BigDecimal requestPrice, Long requestTime, Long timeDiff, String ip,
              String clientType, String clientVersion, String loginName, String comment,
              Long margin, Long equity, Integer marginLevel) {
        this.requestPrice = requestPrice;
        this.requestTime = requestTime;
        this.timeDiff = timeDiff;
        this.ip = ip;
        this.clientType = clientType;
        this.clientVersion = clientVersion;
        this.loginName = loginName;
        this.comment = comment;
        this.margin = margin;
        this.equity = equity;
        this.marginLevel = marginLevel;
    }
    
    /**
     * 简单的 JSON 解析（手工实现，避免依赖外部库）
     * 注意：这是简化版本，生产环境建议使用 Gson 或 Jackson
     */
    public static EventText parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return empty();
        }
        
        try {
            BigDecimal px = extractDecimal(json, "px");
            Long tm = extractLong(json, "tm");
            Long td = extractLong(json, "td");
            String ip = extractString(json, "ip");
            String clt = extractString(json, "clt");
            String clv = extractString(json, "clv");
            String log = extractString(json, "log");
            String cm = extractString(json, "cm");
            Long mg = extractLong(json, "mg");
            Long eqt = extractLong(json, "eqt");
            Integer mgl = extractInt(json, "mgl");
            
            return new EventText(px, tm, td, ip, clt, clv, log, cm, mg, eqt, mgl);
        } catch (Exception e) {
            // 解析失败返回空对象
            return empty();
        }
    }
    
    public static EventText empty() {
        return new EventText(null, null, null, null, null, null, null, null, null, null, null);
    }
    
    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private static Long extractLong(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(-?\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }
    
    private static Integer extractInt(String json, String key) {
        Long val = extractLong(json, key);
        return val != null ? val.intValue() : null;
    }
    
    private static BigDecimal extractDecimal(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([0-9.]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? new BigDecimal(m.group(1)) : null;
    }
    
    /**
     * 检查 comment 是否包含机器人相关关键词
     */
    public boolean containsBotKeyword() {
        if (comment == null) return false;
        String lower = comment.toLowerCase();
        return lower.contains("bot") || lower.contains("ea") || 
               lower.contains("auto") || lower.contains("robot") ||
               lower.contains("algo");
    }
    
    /**
     * 紧凑的构建器模式，可用于在测试或手动创建 EventText 实例时使用。
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private BigDecimal requestPrice;
        private Long requestTime;
        private Long timeDiff;
        private String ip;
        private String clientType;
        private String clientVersion;
        private String loginName;
        private String comment;
        private Long margin;
        private Long equity;
        private Integer marginLevel;
        
        Builder() {
        }
        
        public Builder requestPrice(BigDecimal px) {
            this.requestPrice = px;
            return this;
        }
        
        public Builder requestTime(Long tm) {
            this.requestTime = tm;
            return this;
        }
        
        public Builder timeDiff(Long td) {
            this.timeDiff = td;
            return this;
        }
        
        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }
        
        public Builder clientType(String clt) {
            this.clientType = clt;
            return this;
        }
        
        public Builder clientVersion(String clv) {
            this.clientVersion = clv;
            return this;
        }
        
        public Builder loginName(String log) {
            this.loginName = log;
            return this;
        }
        
        public Builder comment(String cm) {
            this.comment = cm;
            return this;
        }
        
        public Builder margin(Long mg) {
            this.margin = mg;
            return this;
        }
        
        public Builder equity(Long eqt) {
            this.equity = eqt;
            return this;
        }
        
        public Builder marginLevel(Integer mgl) {
            this.marginLevel = mgl;
            return this;
        }
        
        public EventText build() {
            return new EventText(requestPrice, requestTime, timeDiff, ip,
                                 clientType, clientVersion, loginName, comment,
                                 margin, equity, marginLevel);
        }
    }
}