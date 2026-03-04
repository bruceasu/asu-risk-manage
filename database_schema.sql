-- ==============================================================================
-- 交易分析系统增强版 - 数据库架构
-- 版本: 2.0
-- 用途: 持久化实时风控指标和历史分析结果
-- 支持: MySQL 8.0+, MariaDB 10.5+
-- ==============================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS fx_risk_analysis 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE fx_risk_analysis;

-- ==============================================================================
-- 2. 实时指标表（核心特征）
-- ==============================================================================

-- 2.1 账户增强特征表
DROP TABLE IF EXISTS account_features_enhanced;
CREATE TABLE account_features_enhanced (
    -- 基础字段
    account_id BIGINT NOT NULL COMMENT '账户ID',
    capture_time DATETIME(3) NOT NULL COMMENT '捕获时间（毫秒精度）',
    window_start DATETIME(3) NOT NULL COMMENT '统计窗口开始时间',
    window_end DATETIME(3) NOT NULL COMMENT '统计窗口结束时间',
    
    -- 时间维度指标（12个）
    avg_delta_ms DOUBLE COMMENT '平均时间间隔（毫秒）',
    std_delta_ms DOUBLE COMMENT '时间间隔标准差',
    cv_delta DOUBLE COMMENT '时间间隔变异系数（CV）',
    min_delta_ms BIGINT COMMENT '最小时间间隔',
    max_delta_ms BIGINT COMMENT '最大时间间隔',
    median_delta_ms DOUBLE COMMENT '中位数时间间隔',
    p25_delta_ms DOUBLE COMMENT '25分位数',
    p75_delta_ms DOUBLE COMMENT '75分位数',
    p95_delta_ms DOUBLE COMMENT '95分位数',
    pct_lt_300ms DOUBLE COMMENT '低于300ms占比',
    pct_lt_100ms DOUBLE COMMENT '低于100ms占比',
    orders_last_1s INT COMMENT '过去1秒订单数',
    
    -- Request 行为指标（15个）
    avg_request_price_deviation DOUBLE COMMENT '平均请求价格偏差',
    max_request_price_deviation DOUBLE COMMENT '最大请求价格偏差',
    zero_deviation_ratio DOUBLE COMMENT '零偏差比例（可能模拟）',
    avg_time_diff_ms DOUBLE COMMENT '平均时间差（客户端-服务器）',
    std_time_diff_ms DOUBLE COMMENT '时间差标准差',
    negative_time_diff_ratio DOUBLE COMMENT '负时间差占比（客户端时间超前）',
    extreme_delay_ratio DOUBLE COMMENT '极端延迟占比（>1秒）',
    p95_time_diff_ms DOUBLE COMMENT '时间差95分位数',
    unique_client_types INT COMMENT '客户端类型数',
    unique_client_versions INT COMMENT '客户端版本数',
    unique_ips INT COMMENT '唯一IP数',
    unique_login_names INT COMMENT '唯一登录名数',
    has_bot_keyword TINYINT(1) COMMENT '是否包含机器人关键词',
    comment_length_avg DOUBLE COMMENT '平均备注长度',
    comment_empty_ratio DOUBLE COMMENT '空备注比例',
    
    -- 订单状态指标（13个）
    order_count INT COMMENT '订单总数',
    cancel_count INT COMMENT '取消订单数',
    cancel_ratio DOUBLE COMMENT '取消率',
    partial_fill_count INT COMMENT '部分成交数',
    avg_order_lifetime_ms DOUBLE COMMENT '平均订单生命周期（毫秒）',
    median_order_lifetime_ms DOUBLE COMMENT '中位数订单生命周期',
    fast_cancel_count INT COMMENT '快速取消数（<500ms）',
    fast_cancel_ratio DOUBLE COMMENT '快速取消占比',
    pending_to_filled_avg_ms DOUBLE COMMENT '挂单到成交平均时长',
    pending_to_cancel_avg_ms DOUBLE COMMENT '挂单到取消平均时长',
    filled_order_count INT COMMENT '完全成交订单数',
    fill_ratio DOUBLE COMMENT '成交率',
    avg_slippage_bps DOUBLE COMMENT '平均滑点（基点）',
    
    -- 订单参数指标（10个）
    order_size_entropy DOUBLE COMMENT '订单大小熵',
    most_common_size_ratio DOUBLE COMMENT '最常见大小占比',
    integer_size_ratio DOUBLE COMMENT '整数大小占比',
    identical_tpsl_ratio DOUBLE COMMENT '相同止盈止损占比',
    tp_cv DOUBLE COMMENT '止盈变异系数',
    sl_cv DOUBLE COMMENT '止损变异系数',
    avg_tp_distance_bps DOUBLE COMMENT '平均止盈距离（基点）',
    avg_sl_distance_bps DOUBLE COMMENT '平均止损距离（基点）',
    no_tpsl_ratio DOUBLE COMMENT '无止盈止损占比',
    tpsl_equal_ratio DOUBLE COMMENT '止盈等于止损占比',
    
    -- 市场环境指标（6个）
    avg_quote_age_ms DOUBLE COMMENT '平均报价延迟',
    stale_quote_ratio DOUBLE COMMENT '延迟报价占比（>200ms）',
    avg_spread_bps DOUBLE COMMENT '平均点差（基点）',
    spread_widening_count INT COMMENT '点差扩大次数',
    price_jump_count INT COMMENT '价格跳动次数（>10bps）',
    volatile_market_ratio DOUBLE COMMENT '波动市场占比',
    
    -- 综合风险评分（6个）
    markout_500ms DOUBLE COMMENT 'Markout 500毫秒',
    markout_1s DOUBLE COMMENT 'Markout 1秒',
    markout_z_score DOUBLE COMMENT 'Markout Z-score',
    composite_risk_score DOUBLE COMMENT '综合风险分数',
    bot_likelihood_score INT COMMENT '机器人可能性评分（0-100）',
    risk_level VARCHAR(10) COMMENT '风险等级（L0/L1/L2/L3）',
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    
    -- 索引
    PRIMARY KEY (account_id, capture_time),
    KEY idx_capture_time (capture_time),
    KEY idx_bot_likelihood (bot_likelihood_score DESC),
    KEY idx_risk_level (risk_level, capture_time),
    KEY idx_cv_delta (cv_delta)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='账户实时增强特征表（60+指标）';

-- ==============================================================================
-- 3. 跨账户关联表
-- ==============================================================================

-- 3.1 同步交易事件表
DROP TABLE IF EXISTS sync_events;
CREATE TABLE sync_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事件ID',
    bucket_time DATETIME(3) NOT NULL COMMENT '时间桶（500ms精度）',
    symbol VARCHAR(20) NOT NULL COMMENT '交易对',
    side ENUM('BUY', 'SELL') NOT NULL COMMENT '方向',
    account_count INT NOT NULL COMMENT '参与账户数',
    account_ids TEXT NOT NULL COMMENT '账户ID列表（逗号分隔）',
    total_volume DECIMAL(20, 8) COMMENT '总成交量',
    avg_price DECIMAL(20, 8) COMMENT '平均价格',
    sync_strength DOUBLE COMMENT '同步强度（账户数/总活跃账户）',
    is_suspicious TINYINT(1) DEFAULT 0 COMMENT '是否可疑（账户数>=3）',
    detection_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '检测时间',
    
    KEY idx_bucket_time (bucket_time),
    KEY idx_symbol_side (symbol, side),
    KEY idx_suspicious (is_suspicious, account_count DESC),
    KEY idx_detection_time (detection_time)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='跨账户同步交易事件表';

-- 3.2 账户关系表
DROP TABLE IF EXISTS account_relations;
CREATE TABLE account_relations (
    relation_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    account_a BIGINT NOT NULL COMMENT '账户A',
    account_b BIGINT NOT NULL COMMENT '账户B',
    relation_type ENUM('SAME_IP', 'SAME_LOGIN', 'SYNC_TRADING', 'SIMILAR_BEHAVIOR') NOT NULL COMMENT '关系类型',
    similarity_score DOUBLE COMMENT '相似度评分（0-1）',
    first_detected DATETIME(3) COMMENT '首次检测时间',
    last_detected DATETIME(3) COMMENT '最后检测时间',
    occurrence_count INT DEFAULT 1 COMMENT '发生次数',
    evidence_json JSON COMMENT '证据详情（JSON格式）',
    
    UNIQUE KEY uk_account_pair (account_a, account_b, relation_type),
    KEY idx_account_a (account_a),
    KEY idx_account_b (account_b),
    KEY idx_similarity (similarity_score DESC),
    KEY idx_last_detected (last_detected)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='账户关系图谱';

-- ==============================================================================
-- 4. 历史分析结果表
-- ==============================================================================

-- 4.1 离线聚类结果表
DROP TABLE IF EXISTS offline_clusters;
CREATE TABLE offline_clusters (
    cluster_id INT NOT NULL COMMENT '聚类ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    analysis_date DATE NOT NULL COMMENT '分析日期',
    cluster_center_distance DOUBLE COMMENT '到聚类中心距离',
    feature_vector JSON COMMENT '特征向量（JSON格式）',
    cluster_label VARCHAR(50) COMMENT '聚类标签（如：high_freq_bot）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (analysis_date, account_id),
    KEY idx_cluster_id (cluster_id),
    KEY idx_cluster_label (cluster_label)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='离线聚类分析结果';

-- 4.2 Markout 深度分析表
DROP TABLE IF EXISTS markout_aggregates;
CREATE TABLE markout_aggregates (
    agg_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '聚合ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    symbol VARCHAR(20) NOT NULL COMMENT '交易对',
    analysis_date DATE NOT NULL COMMENT '分析日期',
    trade_count INT COMMENT '成交笔数',
    avg_markout_500ms DOUBLE COMMENT '平均Markout 500ms',
    avg_markout_1s DOUBLE COMMENT '平均Markout 1s',
    avg_markout_5s DOUBLE COMMENT '平均Markout 5s',
    pos_ratio_500ms DOUBLE COMMENT '正向Markout比例（500ms）',
    avg_quote_age_ms DOUBLE COMMENT '平均报价延迟',
    stale_quote_ratio DOUBLE COMMENT '延迟报价占比',
    pnl_estimate DECIMAL(20, 8) COMMENT '预估盈亏',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_account_symbol_date (account_id, symbol, analysis_date),
    KEY idx_markout_500ms (avg_markout_500ms DESC),
    KEY idx_analysis_date (analysis_date)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Markout深度分析聚合表';

-- ==============================================================================
-- 5. 告警与日志表
-- ==============================================================================

-- 5.1 实时告警表
DROP TABLE IF EXISTS risk_alerts;
CREATE TABLE risk_alerts (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '告警ID',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    alert_type ENUM('HIGH_RISK', 'BOT_DETECTED', 'SYNC_TRADING', 'STALE_QUOTE', 'ANOMALY') NOT NULL COMMENT '告警类型',
    severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL COMMENT '严重级别',
    message TEXT COMMENT '告警消息',
    indicator_values JSON COMMENT '触发指标值（JSON）',
    alert_time DATETIME(3) NOT NULL COMMENT '告警时间',
    is_handled TINYINT(1) DEFAULT 0 COMMENT '是否已处理',
    handler VARCHAR(50) COMMENT '处理人',
    handle_time DATETIME(3) COMMENT '处理时间',
    handle_note TEXT COMMENT '处理备注',
    
    KEY idx_account_id (account_id),
    KEY idx_alert_time (alert_time DESC),
    KEY idx_severity (severity),
    KEY idx_is_handled (is_handled, alert_time)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='实时风险告警表';

-- 5.2 审计日志表
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    log_time DATETIME(3) NOT NULL COMMENT '日志时间',
    log_level ENUM('DEBUG', 'INFO', 'WARN', 'ERROR') DEFAULT 'INFO' COMMENT '日志级别',
    component VARCHAR(50) COMMENT '组件名称',
    action VARCHAR(100) COMMENT '操作动作',
    account_id BIGINT COMMENT '关联账户ID（可选）',
    details TEXT COMMENT '详细信息',
    execution_time_ms INT COMMENT '执行时长（毫秒）',
    
    KEY idx_log_time (log_time DESC),
    KEY idx_log_level (log_level),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='系统审计日志表';

-- ==============================================================================
-- 6. 视图定义
-- ==============================================================================

-- 6.1 高风险账户视图
CREATE OR REPLACE VIEW v_high_risk_accounts AS
SELECT 
    account_id,
    capture_time,
    bot_likelihood_score,
    risk_level,
    cv_delta,
    identical_tpsl_ratio,
    order_size_entropy,
    avg_request_price_deviation,
    unique_ips,
    unique_login_names,
    markout_500ms,
    composite_risk_score
FROM account_features_enhanced
WHERE 
    bot_likelihood_score >= 70 
    OR risk_level IN ('L2', 'L3')
    OR cv_delta < 0.15
ORDER BY bot_likelihood_score DESC, capture_time DESC;

-- 6.2 最新账户特征视图（每个账户最新一条）
CREATE OR REPLACE VIEW v_latest_account_features AS
SELECT a.*
FROM account_features_enhanced a
INNER JOIN (
    SELECT account_id, MAX(capture_time) AS max_time
    FROM account_features_enhanced
    GROUP BY account_id
) b ON a.account_id = b.account_id AND a.capture_time = b.max_time;

-- 6.3 同步交易可疑事件视图
CREATE OR REPLACE VIEW v_suspicious_sync_events AS
SELECT 
    bucket_time,
    symbol,
    side,
    account_count,
    account_ids,
    total_volume,
    sync_strength,
    detection_time
FROM sync_events
WHERE is_suspicious = 1
ORDER BY detection_time DESC;

-- ==============================================================================
-- 7. 存储过程
-- ==============================================================================

-- 7.1 插入或更新账户特征
DELIMITER //
DROP PROCEDURE IF EXISTS upsert_account_features//
CREATE PROCEDURE upsert_account_features(
    IN p_account_id BIGINT,
    IN p_capture_time DATETIME(3),
    IN p_cv_delta DOUBLE,
    IN p_bot_likelihood_score INT,
    -- 其他参数省略...
    IN p_risk_level VARCHAR(10)
)
BEGIN
    INSERT INTO account_features_enhanced (
        account_id, capture_time, cv_delta, bot_likelihood_score, risk_level
    ) VALUES (
        p_account_id, p_capture_time, p_cv_delta, p_bot_likelihood_score, p_risk_level
    )
    ON DUPLICATE KEY UPDATE
        cv_delta = p_cv_delta,
        bot_likelihood_score = p_bot_likelihood_score,
        risk_level = p_risk_level,
        updated_at = CURRENT_TIMESTAMP;
END//
DELIMITER ;

-- 7.2 生成风险告警
DELIMITER //
DROP PROCEDURE IF EXISTS generate_risk_alert//
CREATE PROCEDURE generate_risk_alert(
    IN p_account_id BIGINT,
    IN p_alert_type VARCHAR(20),
    IN p_severity VARCHAR(10),
    IN p_message TEXT,
    IN p_indicator_values JSON
)
BEGIN
    INSERT INTO risk_alerts (
        account_id, alert_type, severity, message, indicator_values, alert_time
    ) VALUES (
        p_account_id, p_alert_type, p_severity, p_message, p_indicator_values, NOW(3)
    );
END//
DELIMITER ;

-- ==============================================================================
-- 8. 示例查询
-- ==============================================================================

-- 8.1 查询高风险机器人账户（CV < 0.15 且固定策略）
/*
SELECT 
    account_id,
    cv_delta,
    identical_tpsl_ratio,
    order_size_entropy,
    bot_likelihood_score,
    composite_risk_score
FROM v_latest_account_features
WHERE 
    cv_delta < 0.15 
    AND identical_tpsl_ratio > 0.8 
    AND order_size_entropy < 1.0
ORDER BY bot_likelihood_score DESC
LIMIT 100;
*/

-- 8.2 查询同步交易账户组
/*
SELECT 
    bucket_time,
    symbol,
    side,
    account_count,
    account_ids,
    sync_strength
FROM v_suspicious_sync_events
WHERE 
    account_count >= 5 
    AND sync_strength > 0.3
ORDER BY bucket_time DESC
LIMIT 50;
*/

-- 8.3 查询未处理的高严重级别告警
/*
SELECT 
    alert_id,
    account_id,
    alert_type,
    message,
    alert_time
FROM risk_alerts
WHERE 
    is_handled = 0 
    AND severity = 'CRITICAL'
ORDER BY alert_time DESC;
*/

-- 8.4 查询账户关系图谱（账户共享）
/*
SELECT 
    account_a,
    account_b,
    relation_type,
    similarity_score,
    occurrence_count,
    evidence_json
FROM account_relations
WHERE 
    relation_type IN ('SAME_LOGIN', 'SAME_IP') 
    AND occurrence_count > 5
ORDER BY similarity_score DESC;
*/

-- ==============================================================================
-- 9. 数据保留策略
-- ==============================================================================

-- 9.1 删除90天前的特征数据（定期执行）
/*
DELETE FROM account_features_enhanced 
WHERE capture_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
*/

-- 9.2 归档历史告警（180天前）
/*
INSERT INTO risk_alerts_archive 
SELECT * FROM risk_alerts 
WHERE alert_time < DATE_SUB(NOW(), INTERVAL 180 DAY);

DELETE FROM risk_alerts 
WHERE alert_time < DATE_SUB(NOW(), INTERVAL 180 DAY);
*/

-- ==============================================================================
-- 10. 索引优化建议
-- ==============================================================================

-- 如果查询特定时间范围内的高风险账户频繁，可以添加复合索引：
-- ALTER TABLE account_features_enhanced 
--   ADD INDEX idx_capture_bot (capture_time, bot_likelihood_score DESC);

-- 如果需要按风险等级和时间范围查询：
-- ALTER TABLE account_features_enhanced 
--   ADD INDEX idx_risk_capture (risk_level, capture_time DESC);

-- ==============================================================================
-- 结束
-- ==============================================================================
