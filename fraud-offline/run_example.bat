@echo off
setlocal

REM One-click demo runner for Offline FX Replay Tool
REM Build output:
REM   - class files: classes\
REM   - jar file:    bin\app.jar
REM Updated: 2026-02-20 - Enhanced with 60+ bot detection features

pushd "%~dp0"

if not exist classes mkdir classes
if not exist bin mkdir bin
if not exist lib mkdir lib

echo.
echo ================================================================================
echo Offline FX Replay Tool - Enhanced v2.0
echo ================================================================================
echo.

REM Check if lib directory has JAR files (optional, for future JSON parsing)
set CLASSPATH=classes
if exist lib\*.jar (
  echo [Info] Found JAR dependencies in lib\
  for %%f in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%f
)

echo [1/3] Compiling Java sources to classes\ ...
del /q classes\*.class >nul 2>nul
javac -encoding UTF-8 -d classes %CLASSPATH% src\*.java
if errorlevel 1 (
  echo.
  echo [ERROR] Compile failed.
  echo Please check for syntax errors in src\*.java
  popd
  exit /b 1
)
echo [OK] Compilation successful.

echo.
echo [2/3] Packaging JAR to bin\app.jar ...
jar --create --file bin\app.jar --main-class FxReplayPlus -C classes .
if errorlevel 1 (
  echo.
  echo [ERROR] JAR packaging failed.
  popd
  exit /b 1
)
echo [OK] JAR packaged successfully.

@REM echo [3/3] Running replay with example data via JAR ...
@REM java -jar bin\app.jar ^
@REM   --trades examples\trades.csv ^
@REM   --quotes examples\quotes.csv ^
@REM   --out-detail markout_detail.csv ^
@REM   --out-agg markout_agg_by_account_symbol.csv ^
@REM   --agg-account true ^
@REM   --out-agg-account markout_agg_by_account.csv ^
@REM   --time-bucket-min 1 ^
@REM   --bucket-by account_symbol ^
@REM   --out-bucket markout_time_buckets.csv ^
@REM   --quoteage-stats true ^
@REM   --quoteage-scope account_symbol ^
@REM   --quoteage-max-samples 200000 ^
@REM   --out-quoteage quote_age_stats.csv ^
@REM   --cluster true ^
@REM   --cluster-threshold 0.93 ^
@REM   --out-cluster clusters.csv ^
@REM   --baseline true ^
@REM   --out-baseline baseline.csv ^
@REM   --report true ^
@REM   --out-report risk_report.txt ^
@REM   --charts true ^
@REM   --out-chart fx_replay_dashboard.html ^
@REM   --chart-top-n 20 ^
@REM   --top-n 20 ^
@REM   --min-trades 1
@REM if errorlevel 1 (
@REM   echo Run failed.
@REM   popd
@REM   exit /b 1
@REM )

echo.
echo ================================================================================
echo Done. Build artifacts:
echo - classes\*.class        (Compiled classes)
echo - bin\app.jar            (Executable JAR)
echo ================================================================================
echo.
echo Potential analysis outputs (when run):
echo - markout_detail.csv               (Per-trade markout details)
echo - markout_agg_by_account.csv       (Account-level aggregation)
echo - markout_agg_by_account_symbol.csv (Account-Symbol aggregation)
echo - quote_age_stats.csv              (Quote age percentiles)
echo - clusters.csv                     (Account clustering results)
echo - baseline.csv                     (Global baseline statistics)
echo - risk_report.txt                  (Top-N risk accounts report)
echo - fx_replay_dashboard.html         (Interactive visualization)
echo.
echo See 交易分析指标说明文档.md for detailed indicator explanations.
echo.

popd
endlocal

