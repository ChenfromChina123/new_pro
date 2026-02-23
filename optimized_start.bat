@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Project - 优化版一键启动脚本
:: ===========================================

echo.
echo ========================================================
echo              AI Study Project - 优化版一键启动脚本
echo ========================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

:: 检测并杀死占用端口的进程
echo [检测] 检查端口占用情况...

:: 检查并杀死5000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do (
    echo [操作] 发现5000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

:: 检查并杀死3000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000') do (
    echo [操作] 发现3000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo.
echo [1/3] 验证环境依赖...

:: 检查Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请安装Java并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] Java已找到
    java -version 2>&1 | findstr "version" 
)

:: 检查Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未找到Maven，将尝试使用项目内的mvnw
    SET MAVEN_CMD=mvnw
) else (
    echo [成功] Maven已找到
    mvn -version >nul 2>&1
    SET MAVEN_CMD=mvn
)

:: 检查Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Node.js，请安装Node.js并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] Node.js已找到
    node --version
)

:: 检查npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到npm，请安装npm并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] npm已找到
    npm --version
)

echo.
echo [2/3] 启动后端服务 (Spring Boot)...

:: 检查后端目录
if not exist "aispring" (
    echo [错误] 未找到 aispring 目录
    pause
    exit /b 1
)

:: 设置JVM参数
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8"

:: 启动后端服务
echo [启动] 进入 aispring 目录并启动Spring Boot应用...
cd aispring
start "AI Study Backend - Port 5000" cmd /k "title AI Study Backend && echo 后端服务正在启动...请等待约30秒 && %MAVEN_CMD% spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" > ../backend.log 2>&1 && echo 后端服务已退出"
cd ..

timeout /t 5 /nobreak >nul

echo.
echo [3/3] 启动前端服务 (Vue.js)...

:: 检查前端目录
if not exist "vue-app" (
    echo [错误] 未找到 vue-app 目录
    pause
    exit /b 1
)

:: 检查前端依赖
cd vue-app
if not exist "node_modules" (
    echo [提示] 未找到node_modules，正在安装依赖...
    npm install --registry https://registry.npmmirror.com
)

:: 启动前端服务
echo [启动] 进入 vue-app 目录并启动Vue开发服务器...
start "AI Study Frontend - Port 3000" cmd /k "title AI Study Frontend && npm run dev > ../frontend.log 2>&1 && echo 前端服务已退出"
cd ..

echo.
echo ========================================================
echo 服务启动完成！
echo.
echo 后端 API 文档: http://localhost:5000/swagger-ui.html
echo 前端访问地址: http://localhost:3000
echo.
echo 日志文件:
echo   - 后端日志: backend.log
echo   - 前端日志: frontend.log
echo.
echo * 注意事项:
echo   - 关闭弹出的命令行窗口将停止对应服务
echo   - 访问前端页面时可能需要等待后端完全启动
echo ========================================================
echo.

pause