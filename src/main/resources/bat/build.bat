@echo off
REM ============================================
REM Hearthstone-Script Native Image Build Script
REM ============================================

echo [INFO] Starting native image build...
echo.

REM Change to project directory
cd /d "%~dp0..\..\..\..\..\"

echo [INFO] Current directory: %CD%
echo [INFO] Building hs-script-app with GraalVM Native Image...
echo.

REM Run Maven build with tests skipped
call mvn package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo [SUCCESS] Build completed successfully!
    echo ============================================
    echo.
    echo Generated files:
    echo   - hs-script-app\target\hs-script-app.exe
    echo   - hs-script-app\target\*.dll
    echo.
) else (
    echo.
    echo ============================================
    echo [ERROR] Build failed with error code %ERRORLEVEL%
    echo ============================================
    echo.
)

pause
