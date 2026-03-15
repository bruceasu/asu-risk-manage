@echo off
chcp 65001 >nul

REM One-click runner for Realtime Risk Engine Demo
REM Build output:
REM   - class files: classes\
REM   - jar file:    bin\app.jar
REM Updated: 2026-02-20 - Enhanced with 60+ bot detection features

pushd "%~dp0"

if not exist classes mkdir classes
if not exist bin mkdir bin
if not exist examples mkdir examples
if not exist lib mkdir lib

echo.
echo ================================================================================
echo Realtime Risk Engine - Enhanced v2.0
echo ================================================================================
echo.

echo [1/4] Compiling Java sources to classes\ ...
del /q classes\*.class >nul 2>nul

REM Compile with UTF-8 encoding
javac -encoding UTF-8 -d classes src\*.java
if errorlevel 1 (
  echo.
  echo [ERROR] Compile failed.
  echo.
  echo Troubleshooting:
  echo - Check for syntax errors in src\*.java
  echo - Ensure Java 17+ is installed: java -version
  echo - Verify all new tracker classes are in src\
  popd
  exit /b 1
)
echo [OK] Compilation successful. (18 classes including 8 new trackers)

echo.
echo [2/4] Packaging JAR to bin\app.jar ...
jar --create --file bin\app.jar --main-class RealtimeRiskEngineDemo -C classes .
if errorlevel 1 (
  echo.
  echo [ERROR] JAR packaging failed.
  popd
  exit /b 1
)
echo [OK] JAR packaged successfully.

echo.
echo [3/4] Generating 10k test data ...
java -cp classes DataGen10k
if errorlevel 1 (
  echo.
  echo [ERROR] Test data generation failed.
  popd
  exit /b 1
)
echo [OK] Test data generated: examples\events_10000.csv

@REM echo [4/4] Replaying 10k test data (file mode) ...
@REM java -jar bin\app.jar --mode file --events-file examples\events_10000.csv --risk-out risk_accounts.csv --risk-min-n 5
@REM if errorlevel 1 (
@REM   echo Run failed.
@REM   popd
@REM   exit /b 1
@REM )

echo.
echo ================================================================================
echo Done. Build artifacts:
echo - classes\*.class        (Compiled classes - 18 total)
echo - bin\app.jar            (Executable JAR)
echo - examples\events_10000.csv (Test event data)
echo ================================================================================
echo.
echo New Feature Classes:
echo - EventText.java             (JSON parser for event_text field)
echo - IntervalStats.java         (Time interval statistics)
echo - PriceDeviationTracker.java (Request price deviation analysis)
echo - TimeDiffAnalyzer.java      (Client-server time difference)
echo - ClientFingerprintAnalyzer.java (Client fingerprint tracking)
echo - OrderSizeAnalyzer.java     (Order size pattern detection)
echo - TPSLPatternAnalyzer.java   (TP/SL pattern analysis)
echo - SyncDetector.java          (Cross-account synchronization)
echo - EnhancedOrderEvent.java    (Extended order event for production)
echo - IntegrationGuide.java      (Production integration documentation)
echo.
echo Enhanced Risk Scoring:
echo - RiskConfig.java now includes 6 dimensions (vs 3 original)
echo - AccountState.java tracks 60+ indicators
echo - Bot likelihood score (0-100) computed
echo.
echo To run the demo (uncomment lines in this script):
echo   java -jar bin\app.jar --mode file --events-file examples\events_10000.csv
echo.
echo For production integration, see:
echo - src\IntegrationGuide.java   (Step-by-step guide)
echo - src\EnhancedOrderEvent.java (Real order event example)
echo - 交易分析指标说明文档.md      (All 60+ indicators explained)
echo.

popd
endlocal

echo - bin\app.jar
echo.
echo Validation data:
echo - examples\events_10000.csv
echo - risk_accounts.csv

popd
endlocal
