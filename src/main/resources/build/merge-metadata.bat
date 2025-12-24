@echo off
chcp 65001 >nul
REM ============================================
REM GraalVM Native Image Agent - Metadata Collection Script (Merge Mode)
REM ============================================

REM 保存脚本启动时的目录
set "ORIGINAL_DIR=%CD%"

echo [INFO] Starting application with GraalVM Tracing Agent...
echo.

REM 1. 检查 GRAALVM_HOME
if "%GRAALVM_HOME%"=="" (
    echo [ERROR] GRAALVM_HOME 环境变量未设置！
    pause
    exit /b 1
)

REM 2. 检查 java.exe
if not exist "%GRAALVM_HOME%\bin\java.exe" (
    echo [ERROR] 找不到 "%GRAALVM_HOME%\bin\java.exe"
    pause
    exit /b 1
)

REM 3. 切换到 target 目录
cd /d "%~dp0..\..\..\..\target"
echo [INFO] Current directory: %CD%

REM ============================================
REM 自动化查找 ZIP 包
REM ============================================

set "TARGET_ZIP="
REM 查找最新的 hs-script_v*.zip 文件
for /f "delims=" %%i in ('dir /b /o-d "hs-script_v*.zip" 2^>nul') do (
    set "TARGET_ZIP=%%i"
    goto :FoundZip
)

:FoundZip
if "%TARGET_ZIP%"=="" (
    echo [ERROR] 在 target 目录下找不到 hs-script_v*.zip 文件！
    pause
    exit /b 1
)
echo [INFO] 发现安装包: %TARGET_ZIP%

REM ============================================
REM 解压逻辑
REM ============================================

set "DIR_NAME=%TARGET_ZIP:~0,-4%"
set "EXTRACT_PATH=%CD%\%DIR_NAME%"

if exist "%EXTRACT_PATH%" (
    echo [INFO] 目录 "%DIR_NAME%" 已存在，跳过解压。
) else (
    echo [INFO] 正在创建目录并解压...
    powershell -Command "Expand-Archive -Path '%TARGET_ZIP%' -DestinationPath '%EXTRACT_PATH%' -Force"
    if errorlevel 1 (
        echo [ERROR] 解压失败！
        pause
        exit /b 1
    )
)

REM ============================================
REM 定位并切换工作目录
REM ============================================

REM 1. 先查找 JAR 文件名 (只取文件名 %%~nxj)
set "TARGET_JAR_NAME="
for /f "delims=" %%j in ('dir /b /s "%EXTRACT_PATH%\hs-script_v*.jar" 2^>nul') do (
    set "TARGET_JAR_NAME=%%~nxj"
    goto :FoundJar
)

:FoundJar
if "%TARGET_JAR_NAME%"=="" (
    echo [ERROR] 在 "%EXTRACT_PATH%" 中找不到 JAR 文件！
    pause
    exit /b 1
)

REM 2. 【关键】进入解压后的目录
cd /d "%EXTRACT_PATH%"
echo [INFO] 已切换工作目录至: %CD%

echo [INFO] 目标 JAR: %TARGET_JAR_NAME%
REM 注意这里路径变成了 ../../src
echo [INFO] Agent 输出目录: ..\..\src\main\resources\META-INF\native-image
echo.

echo ============================================
echo 请手动操作应用的所有功能...
echo 操作完成后请关闭应用窗口
echo ============================================
echo.

"%GRAALVM_HOME%\bin\java.exe" ^
    -agentlib:native-image-agent=config-merge-dir=../../src/main/resources/META-INF/native-image ^
    -jar "%TARGET_JAR_NAME%"

echo.
echo ============================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Agent 已收集配置
    echo 下一步：运行 build.bat 重新打包 Native Image
) else (
    echo [ERROR] 程序异常退出，错误代码：%ERRORLEVEL%
)
echo ============================================
echo.

REM 回到原始目录
cd /d "%ORIGINAL_DIR%"

pause
