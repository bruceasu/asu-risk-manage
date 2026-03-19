# 生产环境部署指南

## 📋 目录

1. [前置准备](#前置准备)
2. [数据导出配置](#数据导出配置)
3. [定时任务配置](#定时任务配置)
4. [监控与告警](#监控与告警)
5. [故障排查](#故障排查)

---

## 🔧 前置准备

### 1. 服务器要求

**最低配置**:
- CPU: 4核
- 内存: 8GB
- 磁盘: 100GB可用空间
- OS: Linux (CentOS 7+/Ubuntu 18.04+) 或 Windows Server 2016+

**推荐配置** (处理100万+笔/天):
- CPU: 8核+
- 内存: 16GB+
- 磁盘: 500GB SSD
- OS: Linux (CentOS 8/Ubuntu 20.04)

### 2. 软件依赖

```bash
# Linux (CentOS)
sudo yum install -y java-17-openjdk mysql

# Linux (Ubuntu)
sudo apt install -y openjdk-17-jdk mysql-client

# Windows
# 下载安装：
# - JDK 17: https://adoptium.net/
# - MySQL Client: https://dev.mysql.com/downloads/mysql/
```

### 3. 目录结构

```bash
# Linux
mkdir -p /data/trading_analysis/{offline,exports,reports,archives,logs}

# Windows
mkdir D:\TradingAnalysis\offline
mkdir D:\TradingAnalysis\exports
mkdir D:\TradingAnalysis\reports
mkdir D:\TradingAnalysis\archives
mkdir D:\TradingAnalysis\logs
```

### 4. 文件部署

```bash
# 复制offline系统文件
cp -r offline/* /data/trading_analysis/offline/

# 复制SQL脚本
cp export_production_data.sql /data/trading_analysis/
cp export_multi_days.sql /data/trading_analysis/

# 复制自动化脚本
cp auto_export_and_analyze.sh /data/trading_analysis/
chmod +x /data/trading_analysis/auto_export_and_analyze.sh
```

---

## 🔌 数据导出配置

### 方式1: 直接SQL导出 (推荐小数据量)

```bash
# 1. 修改export_production_data.sql中的时间范围
vim /data/trading_analysis/export_production_data.sql

# 2. 执行导出
mysql -h192.168.1.100 -P3306 -urisk_user -p < export_production_data.sql

# 3. 验证导出文件
ls -lh /data/exports/trades_enhanced_yesterday.csv
head -5 /data/exports/trades_enhanced_yesterday.csv
```

**注意事项**:
- MySQL需要 `FILE` 权限: `GRANT FILE ON *.* TO 'risk_user'@'%';`
- 检查 `secure_file_priv` 设置: `SHOW VARIABLES LIKE 'secure_file_priv';`
- 确保输出目录有写权限

### 方式2: 存储过程批量导出 (推荐大数据量)

```sql
-- 1. 创建存储过程
source /data/trading_analysis/export_multi_days.sql

-- 2. 导出最近7天数据
CALL export_trades_by_date_range(
    DATE_SUB(CURDATE(), INTERVAL 7 DAY),
    DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    '/data/exports/'
);

-- 3. 查看导出历史
SELECT * FROM export_log ORDER BY export_date DESC LIMIT 10;
```

### 方式3: Python脚本导出 (最灵活)

如果MySQL `INTO OUTFILE` 受限，可使用Python脚本：

```python
# export_trades.py
import mysql.connector
import csv
from datetime import datetime, timedelta

# 配置
db_config = {
    'host': '192.168.1.100',
    'port': 3306,
    'user': 'risk_user',
    'password': 'password123',
    'database': 'trading_db'
}

# 昨天日期
yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
start_ts = int(datetime.strptime(f'{yesterday} 00:00:00', '%Y-%m-%d %H:%M:%S').timestamp()) * 1000
end_ts = int(datetime.strptime(f'{yesterday} 23:59:59', '%Y-%m-%d %H:%M:%S').timestamp()) * 1000

# 连接数据库
conn = mysql.connector.connect(**db_config)
cursor = conn.cursor()

# 查询
sql = """
SELECT 
    account_id, symbol, 
    CASE WHEN side = 1 THEN 'BUY' ELSE 'SELL' END,
    exec_time_ms, COALESCE(size, 0),
    EventText, COALESCE(order_size, size, 0),
    take_profit, stop_loss
FROM OrderExtPo
WHERE exec_time_ms BETWEEN %s AND %s
ORDER BY exec_time_ms
"""

cursor.execute(sql, (start_ts, end_ts))

# 写入CSV
output_file = f'/data/exports/trades_{yesterday.replace("-", "")}.csv'
with open(output_file, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['account_id','symbol','side','exec_time_ms','size',
                     'eventText','orderSize','takeProfit','stopLoss'])
    writer.writerows(cursor)

print(f'导出完成: {output_file}, {cursor.rowcount} 行')

cursor.close()
conn.close()
```

运行：
```bash
python3 export_trades.py
```

---

## ⏰ 定时任务配置

### Linux - Crontab

```bash
# 1. 编辑crontab
crontab -e

# 2. 添加定时任务（每天凌晨2点执行）
0 2 * * * /data/trading_analysis/auto_export_and_analyze.sh >> /var/log/offline_analysis.log 2>&1

# 3. 验证配置
crontab -l

# 4. 查看日志
tail -f /var/log/offline_analysis.log
```

**其他时间配置示例**:
```bash
# 每小时执行一次
0 * * * * /path/to/script.sh

# 每6小时执行
0 */6 * * * /path/to/script.sh

# 工作日上午9点
0 9 * * 1-5 /path/to/script.sh

# 每月1号凌晨3点
0 3 1 * * /path/to/script.sh
```

### Windows - 任务计划程序

**方式1: GUI配置**
1. 打开 `任务计划程序` (taskschd.msc)
2. 右键 → `创建基本任务`
3. 名称: `离线分析任务`
4. 触发器: `每天 02:00`
5. 操作: `启动程序`
   - 程序: `D:\TradingAnalysis\auto_export_and_analyze.bat`
6. 完成

**方式2: PowerShell命令**
```powershell
# 创建任务
$action = New-ScheduledTaskAction -Execute "D:\TradingAnalysis\auto_export_and_analyze.bat"
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "离线分析任务" -Description "每日Bot检测离线分析"

# 查看任务
Get-ScheduledTask -TaskName "离线分析任务"

# 手动运行测试
Start-ScheduledTask -TaskName "离线分析任务"

# 查看运行历史
Get-ScheduledTask -TaskName "离线分析任务" | Get-ScheduledTaskInfo
```

**方式3: 命令行 (schtasks)**
```cmd
schtasks /create ^
  /tn "离线分析任务" ^
  /tr "D:\TradingAnalysis\auto_export_and_analyze.bat" ^
  /sc daily ^
  /st 02:00 ^
  /ru SYSTEM
```

---

## 📊 监控与告警

### 1. 脚本执行监控

**Linux - 日志监控**
```bash
# 创建监控脚本
cat > /data/trading_analysis/monitor.sh <<'EOF'
#!/bin/bash
LOG_FILE="/var/log/offline_analysis.log"
ERROR_COUNT=$(tail -100 "$LOG_FILE" | grep -c ERROR)

if [ $ERROR_COUNT -gt 0 ]; then
    echo "发现 $ERROR_COUNT 个错误，请检查日志: $LOG_FILE"
    # 发送告警邮件
    mail -s "[告警] 离线分析任务异常" admin@company.com <<< "错误数: $ERROR_COUNT"
fi
EOF

chmod +x /data/trading_analysis/monitor.sh

# 添加到crontab（每小时检查一次）
0 * * * * /data/trading_analysis/monitor.sh
```

**Windows - 事件日志监控**
```powershell
# 创建监控脚本
$script = @'
$logFile = "D:\TradingAnalysis\logs\analysis_$(Get-Date -Format 'yyyyMMdd').log"
if (Test-Path $logFile) {
    $errors = Select-String -Path $logFile -Pattern "ERROR" | Measure-Object
    if ($errors.Count -gt 0) {
        Write-EventLog -LogName Application -Source "OfflineAnalysis" `
            -EventId 1001 -EntryType Error `
            -Message "离线分析发现 $($errors.Count) 个错误"
    }
}
'@
$script | Out-File D:\TradingAnalysis\monitor.ps1

# 添加到任务计划（每小时执行）
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" -Argument "-File D:\TradingAnalysis\monitor.ps1"
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Hours 1)
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "离线分析监控"
```

### 2. 输出文件验证

```bash
# 检查今天是否生成了报告
TODAY=$(date +%Y%m%d)
REPORT_DIR="/data/trading_analysis/reports"

if [ ! -f "${REPORT_DIR}/report_${TODAY}_bot_indicators.csv" ]; then
    echo "WARNING: 今日Bot检测报告未生成"
    # 发送告警
fi

# 统计高风险账户数量
if [ -f "${REPORT_DIR}/report_${TODAY}_bot_indicators.csv" ]; then
    HIGH_RISK=$(awk -F',' 'NR>1 && $3=="1" && $4>80' "${REPORT_DIR}/report_${TODAY}_bot_indicators.csv" | wc -l)
    if [ $HIGH_RISK -gt 50 ]; then
        echo "ALERT: 高风险账户数量异常: $HIGH_RISK"
        # 发送紧急告警
    fi
fi
```

### 3. Prometheus + Grafana监控 (可选)

创建metrics导出脚本：

```bash
# /data/trading_analysis/export_metrics.sh
#!/bin/bash
TODAY=$(date +%Y%m%d)
METRICS_FILE="/var/lib/node_exporter/textfile_collector/offline_analysis.prom"

# 导出指标
cat > "$METRICS_FILE" <<EOF
# HELP offline_analysis_trades_total 处理的交易总数
# TYPE offline_analysis_trades_total gauge
offline_analysis_trades_total $(awk -F',' 'END{print NR-1}' /data/trading_analysis/reports/report_${TODAY}_detail.csv)

# HELP offline_analysis_high_risk_accounts 高风险账户数
# TYPE offline_analysis_high_risk_accounts gauge
offline_analysis_high_risk_accounts $(awk -F',' 'NR>1 && $3=="1" && $4>80' /data/trading_analysis/reports/report_${TODAY}_bot_indicators.csv | wc -l)

# HELP offline_analysis_last_run_timestamp 最后运行时间戳
# TYPE offline_analysis_last_run_timestamp gauge
offline_analysis_last_run_timestamp $(date +%s)
EOF
```

---

## 🔍 故障排查

### 常见问题1: MySQL导出权限错误

**错误**: `ERROR 1290: The MySQL server is running with --secure-file-priv`

**解决**:
```sql
-- 查看允许的导出目录
SHOW VARIABLES LIKE 'secure_file_priv';

-- 方案1: 修改导出路径到允许的目录
INTO OUTFILE '/var/lib/mysql-files/trades.csv'

-- 方案2: 修改MySQL配置 (需要重启)
-- 编辑 /etc/my.cnf 添加:
[mysqld]
secure_file_priv=""

-- 方案3: 使用Python脚本导出（见上方）
```

### 常见问题2: Java编译/运行错误

**错误**: `javac: command not found`

**解决**:
```bash
# 检查Java安装
which java
java -version

# 设置JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 永久设置（添加到~/.bashrc）
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

**错误**: `OutOfMemoryError: Java heap space`

**解决**:
```bash
# 增加堆内存
java -Xmx8G -cp target/classes me.asu.ta.offline.OfflineReplayCliApplication ...

# 或修改脚本中的JAVA_OPTS
JAVA_OPTS="-Xmx8G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 常见问题3: CSV格式错误

**错误**: 导出的CSV中EventText字段JSON解析失败

**解决**:
```sql
-- 检查原始数据
SELECT EventText FROM OrderExtPo WHERE EventText IS NOT NULL LIMIT 1;

-- 如果JSON包含复杂字符，使用Base64编码
SELECT TO_BASE64(EventText) as eventText FROM ...;

-- 在Java中解码
String decoded = new String(Base64.getDecoder().decode(eventTextBase64));
```

### 常见问题4: 磁盘空间不足

**监控与清理**:
```bash
# 检查磁盘使用情况
df -h /data

# 清理30天前的归档
find /data/trading_analysis/archives -name "*.tar.gz" -mtime +30 -delete

# 压缩旧报告
find /data/trading_analysis/reports -name "report_*.csv" -mtime +7 -exec gzip {} \;

# 设置日志轮转 (/etc/logrotate.d/offline_analysis)
/var/log/offline_analysis.log {
    daily
    rotate 7
    compress
    missingok
    notifempty
}
```

### 常见问题5: Crontab不执行

**排查步骤**:
```bash
# 1. 检查crontab是否正确配置
crontab -l

# 2. 检查cron服务状态
systemctl status cron  # Ubuntu/Debian
systemctl status crond  # CentOS/RHEL

# 3. 检查脚本权限
ls -l /data/trading_analysis/auto_export_and_analyze.sh
chmod +x /data/trading_analysis/auto_export_and_analyze.sh

# 4. 使用绝对路径
0 2 * * * /usr/bin/bash /data/trading_analysis/auto_export_and_analyze.sh

# 5. 重定向输出到日志
0 2 * * * /data/trading_analysis/auto_export_and_analyze.sh >> /var/log/cron_offline.log 2>&1

# 6. 手动测试脚本
/data/trading_analysis/auto_export_and_analyze.sh
```

---

## 📞 技术支持

遇到问题时，请收集以下信息：

1. **系统信息**: `uname -a` (Linux) 或 `systeminfo` (Windows)
2. **Java版本**: `java -version`
3. **MySQL版本**: `mysql --version`
4. **错误日志**: 最后50行日志 `tail -50 /var/log/offline_analysis.log`
5. **磁盘空间**: `df -h`
6. **进程状态**: `ps aux | grep java`

联系方式：
- 邮箱: support@company.com
- Slack: #trading-analysis
- 文档: [交易分析指标说明文档.md](../docs/交易分析指标说明文档.md)
