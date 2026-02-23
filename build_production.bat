@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Project - 生产构建脚本
:: ===========================================

echo.
echo ========================================================
echo            AI Study Project - 生产构建脚本
echo ========================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

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

:: 检查Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [成功] Java已找到
) else (
    echo [警告] 未找到Java，跳过后端构建检查
)

:: 检查Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未找到Maven，跳过Maven构建检查
) else (
    echo [成功] Maven已找到
)

echo.
echo [构建] 准备构建生产版本...

:: 检查前端目录
if not exist "vue-app" (
    echo [错误] 未找到 vue-app 目录
    pause
    exit /b 1
)

:: 检查后端目录
if not exist "aispring" (
    echo [错误] 未找到 aispring 目录
    pause
    exit /b 1
)

:: 构建前端
echo.
echo [构建1/2] 正在构建前端项目 (Vue.js)...
cd vue-app

:: 安装依赖
if not exist "node_modules" (
    echo [提示] 安装前端依赖...
    npm install --registry https://registry.npmmirror.com
    if !errorlevel! neq 0 (
        echo [错误] 前端依赖安装失败
        cd ..
        pause
        exit /b 1
    )
)

:: 清理旧构建
if exist "dist" (
    echo [清理] 删除旧的构建文件...
    rmdir /s /q dist
)

:: 执行构建
echo [构建] 执行前端构建 (含代码混淆)...
npm run build
if !errorlevel! neq 0 (
    echo [错误] 前端构建失败
    cd ..
    pause
    exit /b 1
)

echo [完成] 前端构建成功！构建文件位于 vue-app/dist 目录

cd ..

:: 构建后端
echo.
echo [构建2/2] 正在构建后端项目 (Spring Boot)...
cd aispring

:: 使用Maven打包
echo [构建] 执行Maven打包...
if exist "mvnw" (
    ./mvnw clean package -DskipTests
) else (
    mvn clean package -DskipTests
)

if !errorlevel! neq 0 (
    echo [错误] 后端构建失败
    cd ..
    pause
    exit /b 1
)

echo [完成] 后端构建成功！构建文件位于 aispring/target 目录

cd ..

echo.
echo ========================================================
echo 构建完成！
echo.
echo 构建产物:
echo   - 前端: vue-app/dist/ (包含混淆后的代码)
echo   - 后端: aispring/target/*.jar
echo.
echo 部署说明:
echo   1. 将 vue-app/dist/ 的内容部署到Web服务器
echo   2. 运行 java -jar aispring/target/*.jar 启动后端服务
echo ========================================================
echo.

pause