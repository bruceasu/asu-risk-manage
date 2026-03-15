-- ============================================
-- 生产环境数据导出脚本 - MySQL版本
-- 用途：从OrderExtPo表导出增强格式的trades数据供offline分析
-- ============================================

-- 使用说明：
-- 1. 修改时间范围参数（默认导出昨日数据）
-- 2. 修改输出路径 INTO OUTFILE
-- 3. 在MySQL客户端执行: source export_production_data.sql
-- 4. 确保MySQL有文件写权限: GRANT FILE ON *.* TO 'your_user'@'%';

-- ============================================
-- Part 1: 导出Trades数据（增强格式）
-- ============================================

-- 设置变量（方便修改日期范围）
SET @start_time = UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY)) * 1000;  -- 昨日00:00
SET @end_time = UNIX_TIMESTAMP(CURDATE()) * 1000;  -- 今日00:00

-- 显示时间范围（验证）
SELECT 
    '导出时间范围' as info,
    FROM_UNIXTIME(@start_time/1000) as start_date,
    FROM_UNIXTIME(@end_time/1000) as end_date;

-- 预览数据量（执行前检查）
SELECT COUNT(*) as total_trades
FROM OrderExtPo
WHERE exec_time_ms BETWEEN @start_time AND @end_time;

-- 主导出语句
SELECT 
    -- 基础字段（必须）
    account_id,
    symbol,
    CASE 
        WHEN side = 1 OR side = 'BUY' THEN 'BUY'
        WHEN side = 2 OR side = 'SELL' THEN 'SELL'
        ELSE 'UNKNOWN'
    END as side,
    exec_time_ms,
    COALESCE(size, volume, 0) as size,
    
    -- 增强字段（bot检测用）
    -- EventText: JSON字符串，需要转义双引号
    CASE 
        WHEN EventText IS NOT NULL AND EventText != '' THEN
            -- 将JSON中的双引号转义为\"，并去除换行符
            CONCAT(
                '"',
                REPLACE(
                    REPLACE(
                        REPLACE(EventText, '\\', '\\\\'),  -- 先转义反斜杠
                        '"', '""'                          -- 转义双引号（CSV格式）
                    ),
                    '\n', ' '                              -- 去除换行
                ),
                '"'
            )
        ELSE ''
    END as eventText,
    
    -- orderSize: 优先使用dedicated字段，fallback到size
    COALESCE(order_size, size, volume, 0) as orderSize,
    
    -- takeProfit: 可能为NULL
    CASE 
        WHEN take_profit IS NOT NULL AND take_profit > 0 THEN take_profit
        ELSE ''
    END as takeProfit,
    
    -- stopLoss: 可能为NULL
    CASE 
        WHEN stop_loss IS NOT NULL AND stop_loss > 0 THEN stop_loss
        ELSE ''
    END as stopLoss

FROM OrderExtPo
WHERE 
    -- 时间范围
    exec_time_ms BETWEEN @start_time AND @end_time
    
    -- 数据质量过滤
    AND account_id IS NOT NULL 
    AND account_id != ''
    AND symbol IS NOT NULL
    AND symbol != ''
    AND exec_time_ms > 0
    AND (size > 0 OR volume > 0 OR order_size > 0)
    
    -- 可选：只导出特定账户类型
    -- AND account_type IN ('REAL', 'DEMO')
    
    -- 可选：只导出高交易量账户（减少数据量）
    -- AND account_id IN (
    --     SELECT account_id 
    --     FROM OrderExtPo 
    --     WHERE exec_time_ms BETWEEN @start_time AND @end_time
    --     GROUP BY account_id 
    --     HAVING COUNT(*) >= 10
    -- )

ORDER BY exec_time_ms ASC

-- 导出到文件（需要FILE权限）
INTO OUTFILE '/data/exports/trades_enhanced_yesterday.csv'
FIELDS 
    TERMINATED BY ',' 
    OPTIONALLY ENCLOSED BY '"'
    ESCAPED BY '\\'
LINES TERMINATED BY '\n';

-- ============================================
-- Part 2: 导出Quotes数据（如果有单独的quotes表）
-- ============================================

-- 如果quotes在单独的表中，使用类似语句：
/*
SELECT 
    symbol,
    quote_time_ms,
    bid,
    ask
FROM QuoteTick
WHERE 
    quote_time_ms BETWEEN @start_time AND @end_time
    AND symbol IN (
        -- 只导出trades中出现的symbol
        SELECT DISTINCT symbol 
        FROM OrderExtPo 
        WHERE exec_time_ms BETWEEN @start_time AND @end_time
    )
ORDER BY symbol, quote_time_ms

INTO OUTFILE '/data/exports/quotes_yesterday.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '' ESCAPED BY '\\'
LINES TERMINATED BY '\n';
*/

-- ============================================
-- Part 3: 数据验证查询
-- ============================================

-- 统计导出的数据
SELECT 
    '数据统计' as info,
    COUNT(*) as total_records,
    COUNT(DISTINCT account_id) as unique_accounts,
    COUNT(DISTINCT symbol) as unique_symbols,
    MIN(exec_time_ms) as min_time,
    MAX(exec_time_ms) as max_time,
    FROM_UNIXTIME(MIN(exec_time_ms)/1000) as start_datetime,
    FROM_UNIXTIME(MAX(exec_time_ms)/1000) as end_datetime
FROM OrderExtPo
WHERE exec_time_ms BETWEEN @start_time AND @end_time;

-- 检查EventText字段覆盖率
SELECT 
    'EventText覆盖率' as info,
    COUNT(*) as total,
    SUM(CASE WHEN EventText IS NOT NULL AND EventText != '' THEN 1 ELSE 0 END) as with_eventtext,
    ROUND(100.0 * SUM(CASE WHEN EventText IS NOT NULL AND EventText != '' THEN 1 ELSE 0 END) / COUNT(*), 2) as coverage_pct
FROM OrderExtPo
WHERE exec_time_ms BETWEEN @start_time AND @end_time;

-- 检查TP/SL字段覆盖率
SELECT 
    'TP/SL覆盖率' as info,
    COUNT(*) as total,
    SUM(CASE WHEN take_profit IS NOT NULL AND take_profit > 0 THEN 1 ELSE 0 END) as with_tp,
    SUM(CASE WHEN stop_loss IS NOT NULL AND stop_loss > 0 THEN 1 ELSE 0 END) as with_sl,
    SUM(CASE WHEN (take_profit IS NOT NULL OR stop_loss IS NOT NULL) THEN 1 ELSE 0 END) as with_either,
    ROUND(100.0 * SUM(CASE WHEN (take_profit IS NOT NULL OR stop_loss IS NOT NULL) THEN 1 ELSE 0 END) / COUNT(*), 2) as coverage_pct
FROM OrderExtPo
WHERE exec_time_ms BETWEEN @start_time AND @end_time;

-- Top 10 高交易量账户
SELECT 
    account_id,
    COUNT(*) as trade_count,
    COUNT(DISTINCT symbol) as symbols,
    MIN(exec_time_ms) as first_trade,
    MAX(exec_time_ms) as last_trade
FROM OrderExtPo
WHERE exec_time_ms BETWEEN @start_time AND @end_time
GROUP BY account_id
ORDER BY trade_count DESC
LIMIT 10;

-- ============================================
-- 常见问题与解决方案
-- ============================================

/*
Q1: ERROR 1290 (HY000): The MySQL server is running with the --secure-file-priv option
解决：
    -- 查看允许的导出目录
    SHOW VARIABLES LIKE 'secure_file_priv';
    
    -- 修改INTO OUTFILE路径为显示的目录
    -- 或者联系DBA修改my.cnf: secure_file_priv=""

Q2: ERROR 1045 (28000): Access denied; you need the FILE privilege
解决：
    GRANT FILE ON *.* TO 'your_user'@'%';
    FLUSH PRIVILEGES;

Q3: 导出的CSV中EventText字段JSON格式错误
解决：
    -- 检查原始数据
    SELECT EventText FROM OrderExtPo WHERE EventText IS NOT NULL LIMIT 1;
    
    -- 如果JSON本身就包含双引号，需要额外处理
    -- 可以考虑Base64编码：
    -- TO_BASE64(EventText) as eventText

Q4: 导出文件太大（>100MB）
解决：
    -- 方案1: 分批导出（按小时）
    WHERE exec_time_ms BETWEEN @start_time AND @start_time + 3600000
    
    -- 方案2: 只导出高交易量账户
    AND account_id IN (SELECT account_id FROM ... HAVING COUNT(*) >= 100)
    
    -- 方案3: 按账户分片导出
    PARTITION BY HASH(account_id) MOD 10

Q5: 需要导出多天数据
解决：
    -- 创建存储过程循环导出
    -- 见下方 export_multi_days.sql
*/
