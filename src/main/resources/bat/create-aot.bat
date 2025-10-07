@echo off
setlocal
chcp 65001

:: ===== 参数检查 =====
if "%~3"=="" (
    echo 用法: %~nx0 jar路径 aot缓存路径 主类
    echo 示例:
    echo   %~nx0 "xxx.jar" "cds\fs" "xxx.xxx.Application"
    exit /b 1
)

:: ===== 参数变量 =====
set "JAR=%~1"
set "AOTBASE=%~2"
set "MAINCLASS=%~3"

set "AOTCONFIG=%AOTBASE%.aotconfig"
set "AOTCACHE=%AOTBASE%.aot"
set "JAVA=java.exe"

echo ========================================
echo 🔧 JAR        = %JAR%
echo 🔧 AOTCONFIG  = %AOTCONFIG%
echo 🔧 AOTCACHE   = %AOTCACHE%
echo 🔧 MAINCLASS  = %MAINCLASS%
echo ========================================
echo.

:: ===== 清理旧缓存 =====
if exist "%AOTCACHE%" (
    echo 🧹 删除旧缓存 "%AOTCACHE%"
    del /f /q "%AOTCACHE%"
)
if exist "%AOTCONFIG%" (
    echo 🧹 删除旧配置 "%AOTCONFIG%"
    del /f /q "%AOTCONFIG%"
)
echo.

:: ===== record 阶段 =====
echo [1/2] 🚀 开始记录 AOT 配置...
"%JAVA%" -XX:AOTMode=record ^
    -XX:AOTConfiguration="%AOTCONFIG%" ^
    -cp "%JAR%" ^
    %MAINCLASS% ^
    --aot

if errorlevel 1 (
    echo ❌ 记录阶段出错，停止执行。
    exit /b 1
)

echo.

:: ===== create 阶段 =====
echo [2/2] 🧩 创建 AOT 缓存...
"%JAVA%" -XX:AOTMode=create ^
    -XX:AOTConfiguration="%AOTCONFIG%" ^
    -XX:AOTCache="%AOTCACHE%" ^
    -cp "%JAR%" ^
    %MAINCLASS%

if errorlevel 1 (
    echo ❌ 创建阶段出错。
    exit /b 1
)

:: ===== 删除临时的 .aotconfig =====
if exist "%AOTCONFIG%" (
    echo 🗑️  删除临时配置 "%AOTCONFIG%"
    del /f /q "%AOTCONFIG%"
)

echo.
echo ✅ AOT 缓存生成完成: %AOTCACHE%
endlocal
