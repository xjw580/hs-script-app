@echo off
%1 mshta vbscript:CreateObject("Shell.Application").ShellExecute("cmd.exe","/c %~s0 ::","","runas",1)(window.close)&&exit
cd /d %~dp0

set curdir=%cd%

chcp 65001

for /f "delims=\" %%a in ('dir /b /a-d /o-d "%curdir%\*.jar"') do (
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Djna.library.path="%curdir%" -jar %%a %1
)