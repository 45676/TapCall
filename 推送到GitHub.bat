@echo off
chcp 65001 >nul
title 推送到 GitHub
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0push_to_github.ps1"
echo.
pause