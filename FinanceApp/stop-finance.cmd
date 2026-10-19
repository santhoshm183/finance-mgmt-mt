@echo off
setlocal
title Stop Finance Management
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-finance.ps1"
echo.
pause
endlocal
