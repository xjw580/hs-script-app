@echo off
chcp 65001
REM ============================================
REM GraalVM Native Image Agent - Metadata Collection Script
REM ============================================

REM 保存脚本启动时的目录
set "ORIGINAL_DIR=%CD%"

echo [INFO] Starting application with GraalVM Tracing Agent...
echo.

REM 检查 GRAALVM_HOME 是否存在
if "%GRAALVM_HOME%"=="" (
    echo [ERROR] GRAALVM_HOME 环境变量未设置！
    echo 请设置 GraalVM 目录，例如：
    echo   setx GRAALVM_HOME "S:\jdk-25.0.1_graalvm"
    echo 或重新打开终端再试。
    pause
    exit /b 1
)

REM 检查 java.exe 是否存在
if not exist "%GRAALVM_HOME%\bin\java.exe" (
    echo [ERROR] 找不到 "%GRAALVM_HOME%\bin\java.exe"
    echo 请检查 GRAALVM_HOME 是否设置正确。
    pause
    exit /b 1
)

REM Change to target directory
cd /d "%~dp0..\..\..\..\target"

echo [INFO] Current directory: %CD%
echo [INFO] Output directory: ..\src\main\resources\META-INF\native-image
echo.
echo ============================================
echo 请手动操作应用的所有功能：
echo   1. 打开所有设置页面
echo   2. 切换不同的策略选项
echo   3. 添加/删除工作时段规则
echo   4. 保存配置
echo   5. 测试所有按钮和功能
echo.
echo 操作完成后请关闭应用窗口
echo Agent 会自动保存配置到 META-INF/native-image
echo ============================================
echo.

REM Run JAR with GraalVM tracing agent
"%GRAALVM_HOME%\bin\java.exe" ^
    -agentlib:native-image-agent=config-merge-dir=../src/main/resources/META-INF/native-image ^
    -jar hs-script_v4.12.0-GA.jar

echo.
echo ============================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Agent 已收集配置
    echo 配置文件位置：src\main\resources\META-INF/native-image\
    echo.
    echo 下一步：运行 build.bat 重新打包 Native Image
) else (
    echo [ERROR] 程序异常退出，错误代码：%ERRORLEVEL%
)
echo ============================================
echo.

REM 回到原始目录
cd /d "%ORIGINAL_DIR%"

pause
