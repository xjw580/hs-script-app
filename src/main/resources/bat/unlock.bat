@echo off
chcp 65001 >nul

:: 判断是否管理员
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo 请求管理员权限...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

:: ===============================
:: 正常逻辑
:: ===============================
set /p folder=请输入受保护的文件夹路径:

echo.
echo 正在接管所有权...
takeown /f "%folder%" /r /d y

echo.
echo 正在重置权限...
icacls "%folder%" /reset /t /c /l

echo.
echo 尝试移除 Everyone 的拒绝权限...
icacls "%folder%" /remove:d Everyone /t /c

echo.
echo 操作完成！请检查文件夹是否可以访问。
pause
