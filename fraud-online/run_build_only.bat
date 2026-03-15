@echo off
REM Realtime Risk Engine - Build Script
REM Enhanced v2.0 with 60+ bot detection features

pushd "%~dp0"

if not exist classes mkdir classes
if not exist bin mkdir bin
if not exist examples mkdir examples

echo.
echo ===================================
echo Realtime Risk Engine - Build v2.0
echo ===================================
echo.

echo Compiling sources...
del /q classes\*.class 2>nul

::javac -encoding UTF-8 -d classes src\*.java
jbuild -main RealtimeRiskEngine
if errorlevel 1 (
  echo [ERROR] Compilation failed.
  popd
  exit /b 1
)
echo [OK] Compiled successfully.



echo.
echo Done!
echo.
echo Build artifacts:
echo - classes\*.class
echo - bin\app.jar
echo.

popd
