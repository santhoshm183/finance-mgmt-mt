@echo off
setlocal
title Finance Management
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-finance.ps1"
if errorlevel 1 (
    echo.
    echo Finance Management did not start successfully.
)
echo.
pause
endlocal
