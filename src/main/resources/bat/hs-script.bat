@echo off
%1 mshta vbscript:CreateObject("Shell.Application").ShellExecute("cmd.exe","/c %~s0 ::","","runas",1)(window.close)&&exit
cd /d %~dp0

setlocal enabledelayedexpansion

set curdir=%cd%
set MINIMUM_JDK_VERSION=25

chcp 65001 >nul

echo Checking JDK version...

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] JDK not detected
    goto download_jdk
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set jver=%%v
)

set jver=!jver:"=!
for /f "tokens=1 delims=." %%a in ("!jver!") do set major_version=%%a

echo Detected JDK version: !major_version!

if !major_version! lss %MINIMUM_JDK_VERSION% (
    echo [ERROR] JDK version too low, requires at least JDK %MINIMUM_JDK_VERSION%
    goto download_jdk
)

echo JDK version check passed
echo.

for /f "delims=\" %%a in ('dir /b /a-d /o-d "%curdir%\*.jar"') do (
    java -Djna.library.path="%curdir%" -jar %%a %1
)
goto end

:download_jdk
echo.
echo Opening browser to download JDK %MINIMUM_JDK_VERSION%...
timeout /t 2 /nobreak >nul
start https://download.oracle.com/java/%MINIMUM_JDK_VERSION%/latest/jdk-%MINIMUM_JDK_VERSION%_windows-x64_bin.exe
echo.
echo Please download and install JDK, then run this script again
pause
exit /b 1

:end
endlocal