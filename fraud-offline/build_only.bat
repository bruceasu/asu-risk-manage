@echo off
REM ============================================
REM Offline系统快速编译脚本
REM 仅编译，不运行
REM ============================================

cd /d "%~dp0"

echo 清理旧的class文件...
if exist classes rd /s /q classes
mkdir classes

echo 编译所有Java源文件...
javac -encoding UTF-8 -d classes src\*.java

if errorlevel 1 (
    echo ❌ 编译失败！
    pause
    exit /b 1
)

for /f %%a in ('dir /b classes\*.class 2^>nul ^| find /c /v ""') do set COUNT=%%a
echo ✅ 编译成功！生成 %COUNT% 个class文件

pause
