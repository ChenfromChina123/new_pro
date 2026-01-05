@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo       AI Study Project - 一键启动脚本
echo ==========================================

:: 切换到脚本所在目录
cd /d "%~dp0"

:: 检查端口占用
netstat -ano | findstr :5000 >nul
if %errorlevel%==0 (
    echo [提示] 端口 5000 已被占用，后端服务可能已经在运行。
)

netstat -ano | findstr :3000 >nul
if %errorlevel%==0 (
    echo [提示] 端口 3000 已被占用，前端服务可能已经在运行。
)

echo.
echo [1/2] 正在启动后端服务 (Spring Boot)...
:: 使用 start 命令打开新窗口运行后端
start "AI Study Backend - Port 5000" cmd /k "cd aispring && mvn spring-boot:run"

echo [2/2] 正在启动前端服务 (Vue.js)...
:: 使用 start 命令打开新窗口运行前端
start "AI Study Frontend - Port 3000" cmd /k "cd vue-app && npm run dev"

echo.
echo ==========================================
echo 服务正在新窗口中启动，请稍候...
echo 后端 API 文档: http://localhost:5000/swagger-ui.html
echo 前端访问地址: http://localhost:3000
echo.
echo * 注意：关闭弹出的命令行窗口将停止服务 *
echo ==========================================
echo.
pause
