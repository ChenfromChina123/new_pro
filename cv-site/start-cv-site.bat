@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为 UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study CV Site - Windows 简历网站启动脚本
:: 端口：3100
:: ===========================================

echo.
echo ========================================================
echo         AI Study CV Site - 简历网站启动脚本
echo ========================================================
echo.

:: 配置
set CV_PORT=3100
set CV_DIR=%~dp0cv-site
set CV_LOG=%~dp0cv-site.log

:: 检查 Python
where python >nul 2>&1
if %errorlevel% neq 0 (
    where python3 >nul 2>&1
    if %errorlevel% neq 0 (
        echo [错误] 未找到 Python，请安装 Python 并确保其在 PATH 中
        pause
        exit /b 1
    ) else (
        set PYTHON_CMD=python3
    )
) else (
    set PYTHON_CMD=python
)

echo [成功] Python 已找到
%PYTHON_CMD% --version

:: 检查简历网站目录
if not exist "%CV_DIR%" (
    echo [错误] 未找到简历网站目录：%CV_DIR%
    pause
    exit /b 1
) else (
    echo [成功] 简历网站目录已找到
)

cd /d "%CV_DIR%"

:: 检查端口占用
echo [检测] 检查 %CV_PORT% 端口占用情况...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%CV_PORT%') do (
    echo [操作] 发现 %CV_PORT% 端口占用，正在终止 PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

:: 启动服务
echo.
echo [启动] 正在启动简历网站...
echo [提示] 静态 HTML 网站，启动速度很快
echo.

:: 设置端口环境变量
set PORT=%CV_PORT%

:: 启动 Python HTTP 服务器
start "AI Study CV Site - Port %CV_PORT%" cmd /k "
title AI Study CV Site - Port %CV_PORT%
color 0A
echo ========================================================
echo        简历网站服务启动中...
echo        可通过 http://localhost:%CV_PORT% 访问
echo        如需停止服务，请关闭此窗口
echo ========================================================
echo.
cd /d ""%CV_DIR%""
%PYTHON_CMD% -m http.server %CV_PORT%
echo.
echo [提示] 简历网站已停止
echo [提示] 按任意键退出...
pause >nul
"

echo.
echo ========================================================
echo 简历网站已在后台启动
echo 访问地址：http://localhost:%CV_PORT%
echo 域名访问：https://cv.aistudy.icu
echo 日志文件：%CV_LOG%
echo ========================================================
echo.

cd ..

:: 等待端口就绪
echo [等待] 等待服务启动...
timeout /t 5 /nobreak >nul

:: 检查端口是否监听
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%CV_PORT%') do (
    echo [成功] 简历网站已启动 (PID: %%a)
    goto :end
)

echo [警告] 服务可能启动失败，请查看新打开的窗口日志

:end
echo.
echo ========================================================
echo 🛑 停止服务：关闭启动窗口或执行 taskkill /F /IM python.exe
echo 📝 查看日志：type %CV_LOG%
echo ========================================================

pause
