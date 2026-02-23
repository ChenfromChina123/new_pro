@echo off
chcp 65001 >nul

:: 设置JVM参数以优化内存使用
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8"

:: 检查并杀死占用5000端口的进程
echo [检测] 检查5000端口占用情况...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do (
    echo [操作] 发现5000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

:: 设置数据库环境变量
set "DB_USERNAME=root"
set "DB_PASSWORD=123456"
set "DB_NAME=ai_study_db"

echo [启动] 正在启动后端服务...
cd /d d:\Users\Administrator\AistudyProject\new_pro\aispring

:: 使用Maven启动Spring Boot应用，同时输出日志
echo [启动] 启动Spring Boot应用...
start "AI Study Backend - Port 5000" cmd /k "
title AI Study Backend - Port 5000
color 0A
echo ========================================================
echo        AI Study Backend 服务启动中...
echo        请等待约30秒完成初始化
echo        日志将显示在此窗口中
echo ========================================================
echo.
echo 启动参数: !JVM_OPTS!
echo.
mvn spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" --logging.level.com.aispring=INFO
echo.
echo [提示] 后端服务已停止
echo [提示] 按任意键退出...
pause >nul
"
