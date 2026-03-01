@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为 UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Blog - Windows 博客系统启动脚本
:: 端口：3200
:: ===========================================

echo.
echo ========================================================
echo            AI Study Blog - 博客系统启动脚本
echo ========================================================
echo.

:: 配置
set BLOG_PORT=3200
set BLOG_DIR=%~dp0个人博客\tailwind-nextjs-starter-blog-main
set BLOG_LOG=%~dp0blog.log

:: 检查 Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请安装 Node.js 并确保其在 PATH 中
    pause
    exit /b 1
) else (
    echo [成功] Node.js 已找到
    node --version
)

:: 检查博客目录
if not exist "%BLOG_DIR%" (
    echo [错误] 未找到博客系统目录：%BLOG_DIR%
    pause
    exit /b 1
) else (
    echo [成功] 博客系统目录已找到
)

cd /d "%BLOG_DIR%"

:: 检查依赖
if not exist "node_modules" (
    echo [提示] 未找到 node_modules，正在安装依赖...
    call npm install
    if !errorlevel! neq 0 (
        echo [错误] 依赖安装失败
        cd ..
        pause
        exit /b 1
    )
) else (
    echo [成功] node_modules 已存在
)

:: 检查端口占用
echo [检测] 检查 %BLOG_PORT% 端口占用情况...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%BLOG_PORT%') do (
    echo [操作] 发现 %BLOG_PORT% 端口占用，正在终止 PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

:: 启动服务
echo.
echo [启动] 正在启动博客系统...
echo [提示] 首次启动可能需要 30-60 秒构建
echo.

:: 设置端口环境变量
set PORT=%BLOG_PORT%

:: 启动 Next.js
start "AI Study Blog - Port %BLOG_PORT%" cmd /k "
title AI Study Blog - Port %BLOG_PORT%
color 0A
echo ========================================================
echo        AI Study Blog 服务启动中...
echo        预计 30-60 秒后可通过 http://localhost:%BLOG_PORT% 访问
echo        如需停止服务，请关闭此窗口
echo ========================================================
echo.
cd /d ""%BLOG_DIR%""
npm run start
echo.
echo [提示] 博客系统已停止
echo [提示] 按任意键退出...
pause >nul
"

echo.
echo ========================================================
echo 博客系统已在后台启动
echo 访问地址：http://localhost:%BLOG_PORT%
echo 域名访问：https://blog.aistudy.icu
echo 日志文件：%BLOG_LOG%
echo ========================================================
echo.

cd ..

:: 等待端口就绪
echo [等待] 等待服务启动...
timeout /t 10 /nobreak >nul

:: 检查端口是否监听
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%BLOG_PORT%') do (
    echo [成功] 博客系统已启动 (PID: %%a)
    goto :end
)

echo [警告] 服务可能启动失败，请查看新打开的窗口日志
echo [提示] 或查看日志文件：%BLOG_LOG%

:end
echo.
echo ========================================================
echo 🛑 停止服务：关闭启动窗口或执行 taskkill /F /IM node.exe
echo 📝 查看日志：type %BLOG_LOG%
echo ========================================================

pause
