@echo off
REM 快速启动脚本 - 交易分析系统增强版 v2.0
REM 用于快速编译和运行当前版本（基础功能）

echo.
echo ===============================================
echo   交易分析系统增强版 v2.0 - 快速启动
echo ===============================================
echo.

pushd "%~dp0"

REM === 步骤 1: 清理 ===
echo [1/5] 清理旧文件...
if exist classes rmdir /s /q classes 2>nul
mkdir classes

REM === 步骤 2: 编译 ===
echo [2/5] 编译源代码...
javac -encoding UTF-8 -d classes src\*.java
if errorlevel 1 (
    echo.
    echo [错误] 编译失败！
    echo 请检查 Java 版本（需要 JDK 17+）
    pause
    popd
    exit /b 1
)
echo [成功] 编译完成（30 个 .class 文件）

REM === 步骤 3: 生成测试数据 ===
echo [3/5] 生成测试数据...
if not exist examples mkdir examples
java -cp classes DataGen10k > examples\events_10000.csv
if errorlevel 1 (
    echo [警告] 测试数据生成失败，将使用空数据运行
) else (
    echo [成功] 生成 10000 条测试数据
)

REM === 步骤 4: 显示系统信息 ===
echo [4/5] 系统信息
echo.
echo 当前版本功能：
echo   ✅ 时间间隔统计（CV 检测）
echo   ✅ Markout 分析（500ms / 1s）
echo   ✅ Quote Age 监控
echo   ✅ 6维度风险评分
echo   ✅ 机器人可能性评分（0-100）
echo   ⏳ 订单大小熵（需数据扩展）
echo   ⏳ 止盈止损模式（需数据扩展）
echo   ⏳ 客户端指纹（需数据扩展）
echo.
echo 注意：部分指标需要扩展数据源（参见 DataExtensionGuide.java）
echo.

REM === 步骤 5: 运行 ===
echo [5/5] 启动风控引擎...
echo （按 Ctrl+C 停止）
echo.
echo ===============================================
echo.

java -cp "classes;lib\*" RealtimeRiskEngineDemo

echo.
echo ===============================================
echo   运行结束
echo ===============================================
echo.
echo 查看结果：
echo   - risk_accounts.csv     （风险账户快照）
echo   - examples\events_10000.csv  （测试数据）
echo.
echo 下一步：
echo   1. 参考 DataExtensionGuide.java 扩展数据源
echo   2. 参考 代码集成完成报告.md 查看待办事项
echo   3. 参考 交易分析指标说明文档.md 了解所有指标
echo.

popd
pause
