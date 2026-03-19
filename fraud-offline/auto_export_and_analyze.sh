#!/bin/bash
# ============================================
# 自动化数据导出与分析脚本 (Linux)
# 用途：每日定时任务，导出昨日数据并运行离线分析
# 部署：添加到crontab
#   0 2 * * * /path/to/auto_export_and_analyze.sh >> /var/log/offline_analysis.log 2>&1
# ============================================

set -e  # 遇到错误立即退出
set -u  # 使用未定义变量时报错

# ============================================
# 配置参数（根据实际环境修改）
# ============================================

# MySQL配置
DB_HOST="192.168.1.100"
DB_PORT="3306"
DB_USER="risk_user"
DB_PASS="password123"
DB_NAME="trading_db"

# 路径配置
WORKSPACE="/data/trading_analysis"
EXPORT_DIR="${WORKSPACE}/exports"
OFFLINE_DIR="${WORKSPACE}/offline"
REPORT_DIR="${WORKSPACE}/reports"
ARCHIVE_DIR="${WORKSPACE}/archives"

# Java配置
JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
JAVA_OPTS="-Xmx4G -XX:+UseG1GC"

# 时间配置
YESTERDAY=$(date -d "yesterday" +%Y-%m-%d)
DATE_SUFFIX=$(date -d "yesterday" +%Y%m%d)

# 文件名
TRADES_FILE="${EXPORT_DIR}/trades_${DATE_SUFFIX}.csv"
QUOTES_FILE="${EXPORT_DIR}/quotes_${DATE_SUFFIX}.csv"
REPORT_PREFIX="report_${DATE_SUFFIX}"

# 邮件通知（可选）
ENABLE_EMAIL=false
EMAIL_TO="risk-team@company.com"
EMAIL_SUBJECT="[Bot检测] ${YESTERDAY} 离线分析报告"

# ============================================
# 日志函数
# ============================================

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

log_error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1" >&2
}

# ============================================
# 主流程
# ============================================

log "=========================================="
log "开始执行 ${YESTERDAY} 的离线分析任务"
log "=========================================="

# 1. 创建必要的目录
log "创建工作目录..."
mkdir -p "${EXPORT_DIR}" "${REPORT_DIR}" "${ARCHIVE_DIR}"

# 2. 从MySQL导出Trades数据
log "导出Trades数据..."
START_TS=$(date -d "${YESTERDAY} 00:00:00" +%s)000
END_TS=$(date -d "${YESTERDAY} 23:59:59" +%s)999

mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" <<EOF
-- 添加CSV头
SELECT 'account_id','symbol','side','exec_time_ms','size','eventText','orderSize','takeProfit','stopLoss'
UNION ALL
SELECT 
    account_id,
    symbol,
    CASE WHEN side = 1 THEN 'BUY' ELSE 'SELL' END,
    exec_time_ms,
    COALESCE(size, 0),
    CASE WHEN EventText IS NOT NULL THEN 
        CONCAT('"', REPLACE(REPLACE(EventText, '"', '""'), '\n', ' '), '"')
    ELSE '' END,
    COALESCE(order_size, size, 0),
    COALESCE(take_profit, ''),
    COALESCE(stop_loss, '')
FROM OrderExtPo
WHERE exec_time_ms BETWEEN ${START_TS} AND ${END_TS}
    AND account_id IS NOT NULL
    AND symbol IS NOT NULL
ORDER BY exec_time_ms
INTO OUTFILE '${TRADES_FILE}'
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n';
EOF

if [ $? -eq 0 ]; then
    TRADE_COUNT=$(wc -l < "${TRADES_FILE}")
    log "Trades数据导出成功: ${TRADE_COUNT} 行"
else
    log_error "Trades数据导出失败"
    exit 1
fi

# 3. 导出Quotes数据（如果需要）
log "导出Quotes数据..."
# 这里假设quotes在单独的表中，根据实际情况调整
mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" <<EOF
SELECT 'symbol','quote_time_ms','bid','ask'
UNION ALL
SELECT symbol, quote_time_ms, bid, ask
FROM QuoteTick
WHERE quote_time_ms BETWEEN ${START_TS} AND ${END_TS}
    AND symbol IN (
        SELECT DISTINCT symbol FROM OrderExtPo 
        WHERE exec_time_ms BETWEEN ${START_TS} AND ${END_TS}
    )
ORDER BY symbol, quote_time_ms
INTO OUTFILE '${QUOTES_FILE}'
FIELDS TERMINATED BY ',' ENCLOSED BY ''
LINES TERMINATED BY '\n';
EOF

if [ $? -eq 0 ]; then
    QUOTE_COUNT=$(wc -l < "${QUOTES_FILE}")
    log "Quotes数据导出成功: ${QUOTE_COUNT} 行"
else
    log "Warning: Quotes数据导出失败（可能不存在quotes表），使用模拟数据"
    # 如果quotes不存在，可以从trades生成简化版本
    # 这里省略，实际需要根据业务逻辑生成
fi

# 4. 编译offline程序（如果需要）
log "编译offline分析程序..."
cd "${OFFLINE_DIR}"
if [ ! -d "classes" ] || [ -n "$(find src -newer classes -type f 2>/dev/null)" ]; then
    rm -rf classes
    mkdir -p classes
    javac -encoding UTF-8 -d classes src/*.java
    if [ $? -ne 0 ]; then
        log_error "编译失败"
        exit 1
    fi
    log "编译完成"
else
    log "使用已有的class文件"
fi

# 5. 运行离线分析
log "运行离线分析..."
cd "${OFFLINE_DIR}"

${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp classes me.asu.ta.offline.OfflineReplayCliApplication \
    --trades "${TRADES_FILE}" \
    --quotes "${QUOTES_FILE}" \
    --out-detail "${REPORT_DIR}/${REPORT_PREFIX}_detail.csv" \
    --out-agg-as "${REPORT_DIR}/${REPORT_PREFIX}_agg_by_account_symbol.csv" \
    --out-agg-account "${REPORT_DIR}/${REPORT_PREFIX}_agg_by_account.csv" \
    --agg-account true \
    --time-bucket-min 60 \
    --bucket-by all \
    --quoteage-stats true \
    --cluster true \
    --baseline true \
    --report true \
    --out-report "${REPORT_DIR}/${REPORT_PREFIX}_risk_report.txt" \
    --min-trades 10 \
    --top-n 20

if [ $? -eq 0 ]; then
    log "离线分析完成"
else
    log_error "离线分析失败"
    exit 1
fi

# 6. 统计bot_indicators.csv中的高风险账户
log "分析bot检测结果..."
cd "${OFFLINE_DIR}"

if [ -f "bot_indicators.csv" ]; then
    # 移动到报告目录
    mv bot_indicators.csv "${REPORT_DIR}/${REPORT_PREFIX}_bot_indicators.csv"
    
    # 统计高风险账户
    HIGH_RISK=$(awk -F',' 'NR>1 && $3=="1" && $4>80' "${REPORT_DIR}/${REPORT_PREFIX}_bot_indicators.csv" | wc -l)
    MEDIUM_RISK=$(awk -F',' 'NR>1 && $3=="1" && $4>=60 && $4<=80' "${REPORT_DIR}/${REPORT_PREFIX}_bot_indicators.csv" | wc -l)
    
    log "检测结果: 高风险账户=${HIGH_RISK}, 中等风险=${MEDIUM_RISK}"
    
    # 生成TOP10高风险账户列表
    echo "Top 10 高风险Bot账户 (${YESTERDAY})" > "${REPORT_DIR}/${REPORT_PREFIX}_top10_bots.txt"
    echo "==========================================" >> "${REPORT_DIR}/${REPORT_PREFIX}_top10_bots.txt"
    awk -F',' 'NR>1 {print $1, $2, $4, $5}' "${REPORT_DIR}/${REPORT_PREFIX}_bot_indicators.csv" | \
        sort -k3 -rn | head -10 | \
        awk '{printf "账户: %-10s | CV: %-6s | BotScore: %-3s | Entropy: %s\n", $1, $2, $3, $4}' \
        >> "${REPORT_DIR}/${REPORT_PREFIX}_top10_bots.txt"
else
    log "Warning: bot_indicators.csv未生成"
fi

# 7. 压缩归档原始数据（节省空间）
log "归档原始数据..."
tar -czf "${ARCHIVE_DIR}/rawdata_${DATE_SUFFIX}.tar.gz" \
    "${TRADES_FILE}" \
    "${QUOTES_FILE}" \
    2>/dev/null

# 删除原始CSV（已归档）
rm -f "${TRADES_FILE}" "${QUOTES_FILE}"

log "原始数据已归档到 ${ARCHIVE_DIR}/rawdata_${DATE_SUFFIX}.tar.gz"

# 8. 上传报告到服务器（可选）
if command -v curl &> /dev/null; then
    log "上传报告到风控平台..."
    curl -X POST "https://risk-platform.company.com/api/upload" \
        -F "date=${YESTERDAY}" \
        -F "detail=@${REPORT_DIR}/${REPORT_PREFIX}_detail.csv" \
        -F "bot=@${REPORT_DIR}/${REPORT_PREFIX}_bot_indicators.csv" \
        -F "report=@${REPORT_DIR}/${REPORT_PREFIX}_risk_report.txt" \
        -H "Authorization: Bearer YOUR_API_TOKEN" \
        > /dev/null 2>&1
    
    if [ $? -eq 0 ]; then
        log "报告上传成功"
    else
        log "Warning: 报告上传失败"
    fi
fi

# 9. 发送邮件通知（可选）
if [ "${ENABLE_EMAIL}" = true ] && command -v mail &> /dev/null; then
    log "发送邮件通知..."
    {
        echo "离线分析任务完成"
        echo ""
        echo "日期: ${YESTERDAY}"
        echo "交易笔数: ${TRADE_COUNT}"
        echo "高风险账户: ${HIGH_RISK}"
        echo "中等风险账户: ${MEDIUM_RISK}"
        echo ""
        echo "详细报告: ${REPORT_DIR}/${REPORT_PREFIX}_*.csv"
        echo ""
        cat "${REPORT_DIR}/${REPORT_PREFIX}_top10_bots.txt"
    } | mail -s "${EMAIL_SUBJECT}" "${EMAIL_TO}"
fi

# 10. 清理旧文件（保留30天）
log "清理30天前的旧文件..."
find "${REPORT_DIR}" -name "report_*.csv" -mtime +30 -delete
find "${ARCHIVE_DIR}" -name "rawdata_*.tar.gz" -mtime +30 -delete

log "=========================================="
log "任务完成！"
log "报告目录: ${REPORT_DIR}"
log "归档目录: ${ARCHIVE_DIR}"
log "=========================================="

exit 0
