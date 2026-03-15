-- ============================================
-- 多天数据批量导出存储过程
-- 用途：循环导出指定日期范围的数据，每天一个文件
-- ============================================

DELIMITER $$

DROP PROCEDURE IF EXISTS export_trades_by_date_range$$

CREATE PROCEDURE export_trades_by_date_range(
    IN start_date DATE,      -- 开始日期 '2026-02-01'
    IN end_date DATE,        -- 结束日期 '2026-02-10'
    IN output_dir VARCHAR(255)  -- 输出目录 '/data/exports/'
)
BEGIN
    DECLARE current_date DATE;
    DECLARE start_ts BIGINT;
    DECLARE end_ts BIGINT;
    DECLARE output_file VARCHAR(500);
    DECLARE sql_stmt TEXT;
    
    SET current_date = start_date;
    
    -- 循环每一天
    WHILE current_date <= end_date DO
        -- 计算当天的毫秒时间戳
        SET start_ts = UNIX_TIMESTAMP(current_date) * 1000;
        SET end_ts = UNIX_TIMESTAMP(DATE_ADD(current_date, INTERVAL 1 DAY)) * 1000;
        
        -- 构造输出文件名
        SET output_file = CONCAT(output_dir, 'trades_', DATE_FORMAT(current_date, '%Y%m%d'), '.csv');
        
        -- 显示进度
        SELECT CONCAT('正在导出: ', current_date, ' -> ', output_file) as progress;
        
        -- 动态构造SQL（因为INTO OUTFILE路径不能用变量）
        SET @sql = CONCAT(
            'SELECT ',
            '  account_id, symbol, ',
            '  CASE WHEN side = 1 THEN ''BUY'' ELSE ''SELL'' END as side, ',
            '  exec_time_ms, ',
            '  COALESCE(size, 0) as size, ',
            '  CASE WHEN EventText IS NOT NULL THEN CONCAT(''"'', REPLACE(REPLACE(EventText, ''\"'', ''\"\"''), ''\n'', '' ''), ''"'') ELSE '''' END as eventText, ',
            '  COALESCE(order_size, size, 0) as orderSize, ',
            '  COALESCE(take_profit, '''') as takeProfit, ',
            '  COALESCE(stop_loss, '''') as stopLoss ',
            'FROM OrderExtPo ',
            'WHERE exec_time_ms BETWEEN ', start_ts, ' AND ', end_ts, ' ',
            '  AND account_id IS NOT NULL ',
            '  AND symbol IS NOT NULL ',
            'ORDER BY exec_time_ms ',
            'INTO OUTFILE ''', output_file, ''' ',
            'FIELDS TERMINATED BY '','' OPTIONALLY ENCLOSED BY ''"'' ',
            'LINES TERMINATED BY ''\n'''
        );
        
        -- 执行导出
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        -- 记录日志
        INSERT INTO export_log (export_date, filename, record_count, created_at)
        SELECT 
            current_date,
            output_file,
            COUNT(*),
            NOW()
        FROM OrderExtPo
        WHERE exec_time_ms BETWEEN start_ts AND end_ts;
        
        -- 下一天
        SET current_date = DATE_ADD(current_date, INTERVAL 1 DAY);
    END WHILE;
    
    SELECT '导出完成！' as status, 
           DATEDIFF(end_date, start_date) + 1 as total_days;
END$$

DELIMITER ;

-- ============================================
-- 创建导出日志表（可选）
-- ============================================
CREATE TABLE IF NOT EXISTS export_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    export_date DATE NOT NULL,
    filename VARCHAR(500) NOT NULL,
    record_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_export_date (export_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 使用示例
-- ============================================

-- 导出最近7天的数据
CALL export_trades_by_date_range(
    DATE_SUB(CURDATE(), INTERVAL 7 DAY),
    DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    '/data/exports/'
);

-- 导出指定月份的数据
CALL export_trades_by_date_range(
    '2026-02-01',
    '2026-02-28',
    '/data/exports/202602/'
);

-- 查看导出历史
SELECT 
    export_date,
    record_count,
    filename,
    created_at
FROM export_log
ORDER BY export_date DESC
LIMIT 10;
