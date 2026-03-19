@echo off
REM ============================================
REM 自动化数据导出与分析脚本 (Windows)
REM 用途：每日定时任务，导出昨日数据并运行离线分析
REM 部署：添加到Windows任务计划程序
REM   任务计划程序 -> 创建基本任务 -> 每日 02:00 执行此脚本
REM ============================================

setlocal enabledelayedexpansion

REM ============================================
REM 配置参数（根据实际环境修改）
REM ============================================

REM MySQL配置
set DB_HOST=192.168.1.100
set DB_PORT=3306
set DB_USER=risk_user
set DB_PASS=password123
set DB_NAME=trading_db

REM 路径配置
set WORKSPACE=D:\TradingAnalysis
set EXPORT_DIR=%WORKSPACE%\exports
set OFFLINE_DIR=%WORKSPACE%\offline
set REPORT_DIR=%WORKSPACE%\reports
set ARCHIVE_DIR=%WORKSPACE%\archives

REM MySQL客户端路径
set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe

REM Java配置
set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAVA_OPTS=-Xmx4G -XX:+UseG1GC

REM 计算昨天的日期
for /f "tokens=1-3 delims=/ " %%a in ('powershell -command "Get-Date (Get-Date).AddDays(-1) -Format 'yyyy-MM-dd'"') do (
    set YESTERDAY=%%a
)
for /f "tokens=1-3 delims=/ " %%a in ('powershell -command "Get-Date (Get-Date).AddDays(-1) -Format 'yyyyMMdd'"') do (
    set DATE_SUFFIX=%%a
)

REM 文件名
set TRADES_FILE=%EXPORT_DIR%\trades_%DATE_SUFFIX%.csv
set QUOTES_FILE=%EXPORT_DIR%\quotes_%DATE_SUFFIX%.csv
set REPORT_PREFIX=report_%DATE_SUFFIX%

REM 日志文件
set LOG_FILE=%WORKSPACE%\logs\analysis_%DATE_SUFFIX%.log

REM ============================================
REM 日志函数
REM ============================================

:log
    echo [%date% %time%] %~1 >> "%LOG_FILE%"
    echo [%date% %time%] %~1
    goto :eof

:log_error
    echo [%date% %time%] ERROR: %~1 >> "%LOG_FILE%"
    echo [%date% %time%] ERROR: %~1
    goto :eof

REM ============================================
REM 主流程
REM ============================================

call :log "=========================================="
call :log "开始执行 %YESTERDAY% 的离线分析任务"
call :log "=========================================="

REM 1. 创建必要的目录
call :log "创建工作目录..."
if not exist "%EXPORT_DIR%" mkdir "%EXPORT_DIR%"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"
if not exist "%ARCHIVE_DIR%" mkdir "%ARCHIVE_DIR%"
if not exist "%WORKSPACE%\logs" mkdir "%WORKSPACE%\logs"

REM 2. 计算时间戳（毫秒）
for /f %%i in ('powershell -command "[int64](Get-Date '%YESTERDAY% 00:00:00').ToUniversalTime().Subtract([datetime]'1970-01-01 00:00:00').TotalSeconds * 1000"') do set START_TS=%%i
for /f %%i in ('powershell -command "[int64](Get-Date '%YESTERDAY% 23:59:59').ToUniversalTime().Subtract([datetime]'1970-01-01 00:00:00').TotalSeconds * 1000 + 999"') do set END_TS=%%i

call :log "时间范围: %START_TS% - %END_TS%"

REM 3. 从MySQL导出Trades数据
call :log "导出Trades数据..."

REM 创建临时SQL文件
set TEMP_SQL=%TEMP%\export_trades_%DATE_SUFFIX%.sql
echo SELECT 'account_id','symbol','side','exec_time_ms','size','eventText','orderSize','takeProfit','stopLoss' > "%TEMP_SQL%"
echo UNION ALL >> "%TEMP_SQL%"
echo SELECT >> "%TEMP_SQL%"
echo     account_id, >> "%TEMP_SQL%"
echo     symbol, >> "%TEMP_SQL%"
echo     CASE WHEN side = 1 THEN 'BUY' ELSE 'SELL' END, >> "%TEMP_SQL%"
echo     exec_time_ms, >> "%TEMP_SQL%"
echo     COALESCE(size, 0), >> "%TEMP_SQL%"
echo     CASE WHEN EventText IS NOT NULL THEN >> "%TEMP_SQL%"
echo         CONCAT('\"', REPLACE(REPLACE(EventText, '\"', '\"\"'), '\n', ' '), '\"') >> "%TEMP_SQL%"
echo     ELSE '' END, >> "%TEMP_SQL%"
echo     COALESCE(order_size, size, 0), >> "%TEMP_SQL%"
echo     COALESCE(take_profit, ''), >> "%TEMP_SQL%"
echo     COALESCE(stop_loss, '') >> "%TEMP_SQL%"
echo FROM OrderExtPo >> "%TEMP_SQL%"
echo WHERE exec_time_ms BETWEEN %START_TS% AND %END_TS% >> "%TEMP_SQL%"
echo     AND account_id IS NOT NULL >> "%TEMP_SQL%"
echo     AND symbol IS NOT NULL >> "%TEMP_SQL%"
echo ORDER BY exec_time_ms >> "%TEMP_SQL%"
echo INTO OUTFILE '%TRADES_FILE:\=/%' >> "%TEMP_SQL%"
echo FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' >> "%TEMP_SQL%"
echo LINES TERMINATED BY '\n'; >> "%TEMP_SQL%"

"%MYSQL_BIN%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%TEMP_SQL%" 2>> "%LOG_FILE%"

if %ERRORLEVEL% equ 0 (
    for /f %%i in ('powershell -command "(Get-Content '%TRADES_FILE%').Count"') do set TRADE_COUNT=%%i
    call :log "Trades数据导出成功: !TRADE_COUNT! 行"
) else (
    call :log_error "Trades数据导出失败"
    del "%TEMP_SQL%"
    exit /b 1
)

del "%TEMP_SQL%"

REM 4. 编译offline程序（如果需要）
call :log "检查offline程序..."
cd /d "%OFFLINE_DIR%"

if not exist "classes" (
    call :log "编译offline分析程序..."
    if exist classes rd /s /q classes
    mkdir classes
    javac -encoding UTF-8 -d classes src\*.java 2>> "%LOG_FILE%"
    if !ERRORLEVEL! neq 0 (
        call :log_error "编译失败"
        exit /b 1
    )
    call :log "编译完成"
) else (
    call :log "使用已有的class文件"
)

REM 5. 运行离线分析
call :log "运行离线分析..."

"%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% -cp classes me.asu.ta.offline.OfflineReplayCliApplication ^
    --trades "%TRADES_FILE%" ^
    --quotes "%QUOTES_FILE%" ^
    --out-detail "%REPORT_DIR%\%REPORT_PREFIX%_detail.csv" ^
    --out-agg-as "%REPORT_DIR%\%REPORT_PREFIX%_agg_by_account_symbol.csv" ^
    --out-agg-account "%REPORT_DIR%\%REPORT_PREFIX%_agg_by_account.csv" ^
    --agg-account true ^
    --time-bucket-min 60 ^
    --bucket-by all ^
    --quoteage-stats true ^
    --cluster true ^
    --baseline true ^
    --report true ^
    --out-report "%REPORT_DIR%\%REPORT_PREFIX%_risk_report.txt" ^
    --min-trades 10 ^
    --top-n 20 >> "%LOG_FILE%" 2>&1

if %ERRORLEVEL% equ 0 (
    call :log "离线分析完成"
) else (
    call :log_error "离线分析失败"
    exit /b 1
)

REM 6. 处理bot_indicators.csv
call :log "分析bot检测结果..."

if exist "bot_indicators.csv" (
    move /y bot_indicators.csv "%REPORT_DIR%\%REPORT_PREFIX%_bot_indicators.csv" > nul
    
    REM 统计高风险账户（使用PowerShell）
    powershell -command "$csv = Import-Csv '%REPORT_DIR%\%REPORT_PREFIX%_bot_indicators.csv'; $high = ($csv | Where-Object {$_.isBotLike -eq '1' -and [int]$_.botScore -gt 80}).Count; $medium = ($csv | Where-Object {$_.isBotLike -eq '1' -and [int]$_.botScore -ge 60 -and [int]$_.botScore -le 80}).Count; Write-Host \"高风险账户: $high, 中等风险: $medium\"" >> "%LOG_FILE%"
    
    REM 生成TOP10高风险账户列表
    powershell -command "$csv = Import-Csv '%REPORT_DIR%\%REPORT_PREFIX%_bot_indicators.csv' | Sort-Object {[int]$_.botScore} -Descending | Select-Object -First 10; 'Top 10 高风险Bot账户 (%YESTERDAY%)' | Out-File '%REPORT_DIR%\%REPORT_PREFIX%_top10_bots.txt'; '==========================================' | Out-File '%REPORT_DIR%\%REPORT_PREFIX%_top10_bots.txt' -Append; $csv | ForEach-Object {\"账户: {0,-10} | CV: {1,-6} | BotScore: {2,-3} | Entropy: {3}\" -f $_.account_id, $_.cv, $_.botScore, $_.entropy} | Out-File '%REPORT_DIR%\%REPORT_PREFIX%_top10_bots.txt' -Append"
    
    call :log "Bot检测报告已生成"
) else (
    call :log "Warning: bot_indicators.csv未生成"
)

REM 7. 压缩归档原始数据
call :log "归档原始数据..."
powershell -command "Compress-Archive -Path '%TRADES_FILE%','%QUOTES_FILE%' -DestinationPath '%ARCHIVE_DIR%\rawdata_%DATE_SUFFIX%.zip' -Force" 2>> "%LOG_FILE%"

if %ERRORLEVEL% equ 0 (
    del "%TRADES_FILE%" "%QUOTES_FILE%" 2>nul
    call :log "原始数据已归档到 %ARCHIVE_DIR%\rawdata_%DATE_SUFFIX%.zip"
) else (
    call :log "Warning: 数据归档失败"
)

REM 8. 清理30天前的旧文件
call :log "清理30天前的旧文件..."
forfiles /p "%REPORT_DIR%" /m report_*.csv /d -30 /c "cmd /c del @path" 2>nul
forfiles /p "%ARCHIVE_DIR%" /m rawdata_*.zip /d -30 /c "cmd /c del @path" 2>nul

REM 9. 发送邮件通知（可选，使用PowerShell）
REM powershell -command "Send-MailMessage -To 'team@company.com' -From 'bot-detect@company.com' -Subject '[Bot检测] %YESTERDAY% 离线分析报告' -Body (Get-Content '%REPORT_DIR%\%REPORT_PREFIX%_top10_bots.txt' | Out-String) -SmtpServer 'smtp.company.com'"

call :log "=========================================="
call :log "任务完成！"
call :log "报告目录: %REPORT_DIR%"
call :log "归档目录: %ARCHIVE_DIR%"
call :log "日志文件: %LOG_FILE%"
call :log "=========================================="

endlocal
exit /b 0
