@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Project - 优化版前端启动脚本
:: ===========================================

echo.
echo ========================================================
echo            AI Study Project - 优化版前端启动脚本
echo ========================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

:: 检查并杀死占用端口的进程
echo [检测] 检查3000端口占用情况...

:: 检查并杀死3000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000') do (
    echo [操作] 发现3000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo.
echo [环境检测] 验证必要组件...

:: 检查Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Node.js，请安装Node.js并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] Node.js已找到
)

:: 检查npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到npm，请安装npm并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] npm已找到
)

:: 检查前端目录
if not exist "ai-tutor-system\vue-app" (
    echo [错误] 未找到 ai-tutor-system\vue-app 目录
    pause
    exit /b 1
)

echo.
echo [依赖检测] 检查前端依赖...

:: 检查前端依赖
cd ai-tutor-system\vue-app
if not exist "node_modules" (
    echo [提示] 未找到node_modules，正在安装依赖...
    npm install --registry https://registry.npmmirror.com
    if !errorlevel! neq 0 (
        echo [错误] 依赖安装失败
        cd ..
        pause
        exit /b 1
    )
) else (
    echo [成功] node_modules已存在
)

:: 检查package.json中的dev脚本
findstr /C:"dev" package.json >nul
if %errorlevel% neq 0 (
    echo [警告] package.json中未找到dev脚本
) else (
    echo [成功] 找到dev启动脚本
)

cd ..

echo.
echo [启动] 正在启动前端服务...

:: 启动前端服务
echo [启动] 进入 ai-tutor-system\vue-app 目录并启动Vue开发服务器...
echo [提示] 前端服务将在新窗口中启动，预计3-5秒后可用...
echo [提示] 访问 http://localhost:3000 查看前端界面
echo.

:: 启动前端服务
start "AI Study Frontend - Port 3000" cmd /k "
title AI Study Frontend - Port 3000
color 0B
echo ========================================================
echo        AI Study Frontend 服务启动中...
echo        预计3-5秒后可通过 http://localhost:3000 访问
echo        如需停止服务，请关闭此窗口
echo ========================================================
echo.
cd /d ""%~dp0ai-tutor-system\vue-app""
npm run dev
echo.
echo [提示] 前端服务已停止
echo [提示] 按任意键退出...
pause >nul
"

echo.
echo ========================================================
echo 前端服务已启动！
echo.
echo 服务地址: http://localhost:3000
echo.
echo * 如需停止服务，请关闭启动前端的命令行窗口 *
echo * 如需停止后端服务，请找到对应后端窗口并关闭 *
echo ========================================================
echo.

pause
