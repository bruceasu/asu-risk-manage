@echo off
REM ============================================
REM Offline系统编译与运行脚本（增强版）
REM 包含bot检测功能
REM ============================================

cd /d "%~dp0"

echo ========================================
echo 步骤1: 清理旧的编译文件
echo ========================================
if exist classes rd /s /q classes
mkdir classes
echo 清理完成！
echo.

echo ========================================
echo 步骤2: 编译所有Java源文件
echo ========================================
javac -encoding UTF-8 -d classes src\*.java
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo ========================================
echo 步骤3: 统计编译结果
echo ========================================
for /f %%a in ('dir /b classes\*.class 2^>nul ^| find /c /v ""') do set COUNT=%%a
echo ✅ 成功编译 %COUNT% 个class文件
echo.

echo ========================================
echo 步骤4: 运行基础示例（使用原始数据）
echo ========================================
echo 注意：原始trades.csv不包含bot检测字段，bot指标列将为空
java -cp classes FxReplayPlus --trades examples\trades.csv --quotes examples\quotes.csv --agg-account true
echo.

echo ========================================
echo 步骤5: 运行增强示例（使用新数据格式）
echo ========================================
echo 注意：trades_enhanced.csv包含完整的bot检测字段
java -cp classes FxReplayPlus --trades examples\trades_enhanced.csv --quotes examples\quotes.csv --agg-account true --min-trades 0
echo.

echo ========================================
echo 完成！生成的文件：
echo ========================================
echo 1. markout_detail.csv - 逐笔明细（含bot指标）
echo 2. markout_agg_by_account_symbol.csv - 按账户+品种聚合
echo 3. markout_agg_by_account.csv - 按账户聚合
echo 4. bot_indicators.csv - bot检测指标专项报告
echo 5. baseline.csv - 全局基线
echo.
echo 建议查看：
echo - bot_indicators.csv 查看账户bot评分排名
echo - markout_detail.csv 查看每笔交易的bot指标
echo ========================================

pause
