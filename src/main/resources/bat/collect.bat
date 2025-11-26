@echo off
REM ============================================
REM GraalVM Native Image Agent - Metadata Collection Script
REM ============================================
REM
REM 使用说明：
REM 1. 运行此脚本启动应用
REM 2. 手动操作应用的所有功能（点击所有按钮、打开所有设置页面）
REM 3. 关闭应用，Agent 会自动将配置写入到 META-INF/native-image
REM 4. 运行 build.bat 重新打包
REM
REM ============================================

echo [INFO] Starting application with GraalVM Tracing Agent...
echo.

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
REM 添加 -Dfastjson2 参数禁用 ASM，避免动态类生成错误
"S:\jdk-25.0.1_graalvm\bin\java.exe" ^
    -Dfastjson2.creator=reflect ^
    -Dfastjson2.writer=reflect ^
    -Dfastjson2.reader=reflect ^
    -agentlib:native-image-agent=config-output-dir=../src/main/resources/META-INF/native-image ^
    -jar hs-script_v4.12.0-GA.jar

echo.
echo ============================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Agent 已收集配置
    echo 配置文件位置：src\main\resources\META-INF\native-image\
    echo.
    echo 下一步：运行 build.bat 重新打包 Native Image
) else (
    echo [ERROR] 程序异常退出，错误代码：%ERRORLEVEL%
)
echo ============================================
echo.

pause
