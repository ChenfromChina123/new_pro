@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为 UTF-8
chcp 65001 >nul

echo.
echo ========================================================
echo           AI Study Project - 全栈启动脚本
echo ========================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

echo [1/3] 验证环境依赖...

:: 检查 Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java，请安装 Java 并确保其在 PATH 中
    pause
    exit /b 1
) else (
    echo [成功] Java 已找到
)

:: 检查 Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请安装 Node.js 并确保其在 PATH 中
    pause
    exit /b 1
) else (
    echo [成功] Node.js 已找到
)

echo.
echo [2/3] 启动后端服务 (Spring Boot)...

:: 检查后端目录
if not exist "ai-tutor-system\aispring" (
    echo [错误] 未找到 ai-tutor-system\aispring 目录
    pause
    exit /b 1
)

:: 设置 JVM 参数
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8"

:: 启动后端服务
cd ai-tutor-system\aispring
start "AI Study Backend - Port 5000" cmd /k "title AI Study Backend && echo 后端服务正在启动...请等待约 30 秒 && mvn spring-boot:run -Dspring-boot.run.jvmArguments=%JVM_OPTS% && pause"
cd ..

timeout /t 5 /nobreak >nul

echo.
echo [3/3] 启动前端服务 (Vue.js)...

:: 检查前端目录
if not exist "ai-tutor-system\vue-app" (
    echo [错误] 未找到 ai-tutor-system\vue-app 目录
    pause
    exit /b 1
)

:: 启动前端服务
cd ai-tutor-system\vue-app
start "AI Study Frontend - Port 3000" cmd /k "title AI Study Frontend && npm run dev && pause"
cd ..

echo.
echo ========================================================
echo 服务启动完成！
echo.
echo 后端 API 文档：http://localhost:5000/swagger-ui.html
echo 前端访问地址：http://localhost:3000
echo.
echo 注意：服务将在独立窗口中运行，关闭窗口将停止对应服务
echo ========================================================
echo.

timeout /t 3 /nobreak >nul
