@echo off
setlocal enabledelayedexpansion

:: ==============================================================================
:: word-game Windows 启动脚本
:: 功能：快速启动前端开发服务器和后端 API 服务
:: ==============================================================================

echo [1/2] 正在启动后端 API 服务 (端口 5201)...
start "Word-Game Backend" /D "%~dp0" cmd /k "npm run server"

echo [2/2] 正在启动前端开发服务器 (端口 5200)...
start "Word-Game Frontend" /D "%~dp0" cmd /k "npm run dev"

echo.
echo ============================================================
echo 服务启动中...
echo 前端地址: http://localhost:5200
echo 后端地址: http://127.0.0.1:5201
echo ============================================================
echo.
pause
