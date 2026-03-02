@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Project - 优化版后端启动脚本
:: ===========================================

echo.
echo ========================================================
echo            AI Study Project - 优化版后端启动脚本
echo ========================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

:: 检查并杀死占用端口的进程
echo [检测] 检查5000端口占用情况...

:: 检查并杀死5000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do (
    echo [操作] 发现5000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo.
echo [环境检测] 验证必要组件...

:: 检查Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请安装Java并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] Java已找到
)

:: 检查Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未找到Maven，将尝试使用项目内的mvnw
    SET MAVEN_CMD=mvnw
) else (
    echo [成功] Maven已找到
    SET MAVEN_CMD=mvn
)

:: 检查后端目录
if not exist "ai-tutor-system\aispring" (
    echo [错误] 未找到 ai-tutor-system\aispring 目录
    pause
    exit /b 1
)

:: 设置数据库环境变量
set "SPRING_PROFILES_ACTIVE=prod"
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=ai_study_db"
set "DB_USERNAME=root"
set "DB_PASSWORD=123456"

:: 设置JVM参数
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

:: 设置Spring Boot参数
set "SPRING_OPTS=--server.port=5000 --spring.profiles.active=!SPRING_PROFILES_ACTIVE! --logging.level.com.aispring=INFO --logging.file.name=logs/backend.log"

echo.
echo [启动] 正在启动后端服务...
echo     JVM参数: !JVM_OPTS!
echo     Spring参数: !SPRING_OPTS!
echo.

:: 进入后端目录并启动服务
cd ai-tutor-system\aispring

:: 创建日志目录
if not exist "logs" mkdir logs

:: 启动Spring Boot应用
echo [启动] 执行: %MAVEN_CMD% spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" !SPRING_OPTS!
echo.
echo [提示] 后端服务将在新窗口中启动，请稍候约30秒完成初始化...
echo [提示] 访问 http://localhost:5000/swagger-ui.html 查看API文档
echo.
start "AI Study Backend - Port 5000" cmd /k "
title AI Study Backend - Port 5000
color 0A
echo ========================================================
echo        AI Study Backend 服务启动中...
echo        请等待约30-45秒完成初始化
echo        日志将保存到 logs/backend.log
echo        如需停止服务，请关闭此窗口
echo ========================================================
echo.
%MAVEN_CMD% spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" !SPRING_OPTS!
echo.
echo [提示] 后端服务已停止
echo [提示] 按任意键退出...
pause >nul
"

cd ..

echo.
echo ========================================================
echo 后端服务已启动！
echo.
echo 服务地址: http://localhost:5000
echo API文档: http://localhost:5000/swagger-ui.html
echo 管理后台: http://localhost:5000/admin
echo 日志文件: aispring/logs/backend.log
echo.
echo * 如需停止服务，请关闭启动后端的命令行窗口 *
echo ========================================================
echo.

pause
