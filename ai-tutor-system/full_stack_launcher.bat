@echo off
setlocal enabledelayedexpansion

:: 设置控制台编码为UTF-8
chcp 65001 >nul

:: ===========================================
::  AI Study Project - 完整版一键启动脚本
::  包含环境检测、代码更新、服务启动功能
:: ===========================================

echo.
echo =========================================================================
echo                    AI Study Project - 完整版一键启动脚本
echo =========================================================================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

echo [时间] %date% %time%
echo.

:: ==================== 环境检测 ====================
echo [阶段1/4] 环境检测与验证...
echo.

:: 检查Git
where git >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Git，请安装Git并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] Git已找到
    for /f "tokens=*" %%i in ('git --version') do echo [版本] %%i
)

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
    echo [版本] Node: && node --version
)

:: 检查npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到npm，请安装npm并确保其在PATH中
    pause
    exit /b 1
) else (
    echo [成功] npm已找到
    echo [版本] NPM: && npm --version
)

:: 检查磁盘空间
for /f "tokens=3" %%a in ('dir /-c') do set "freespace=%%a"
if defined freespacetemp (
    set "freespace=!freespace:,=!"
    set /a "freespace_gb=!freespace!/1024/1024/1024"
    if !freespace_gb! LSS 2 (
        echo [警告] 磁盘空间不足，建议至少保留2GB空闲空间
    ) else (
        echo [成功] 磁盘空间充足 (!freespace_gb!GB可用)
    )
) else (
    echo [信息] 无法检测磁盘空间
)

:: ==================== 代码更新 ====================
echo.
echo [阶段2/4] 检查并更新代码...
echo.

:: 检查是否为Git仓库
if not exist ".git" (
    echo [错误] 当前目录不是Git仓库
    pause
    exit /b 1
)

:: 获取当前分支
for /f "tokens=*" %%i in ('git branch ^| findstr "^*"') do set "current_branch=%%i"
set "current_branch=!current_branch:* =!"

echo [信息] 当前分支: !current_branch!

:: 暂存当前更改（如果有）
git add .
git status --porcelain | findstr/lookingfor "^.M\|^.A\|^.D\|^.R\|^.C\|^.U"
if !errorlevel! equ 0 (
    echo [操作] 检测到本地更改，正在暂存...
    git stash push -m "Auto-stash before update $(date +%Y%m%d_%H%M%S)"
)

:: 获取远程更新
echo [操作] 获取远程更新...
git fetch origin
if !errorlevel! neq 0 (
    echo [警告] 无法获取远程更新，可能网络问题
) else (
    :: 检查是否有更新
    for /f %%i in ('git rev-list HEAD...origin/!current_branch! --count 2^>nul') do set "update_count=%%i"
    if "!update_count!" gtr "0" (
        echo [操作] 检测到 !update_count! 个更新，正在合并...
        git merge origin/!current_branch!
        if !errorlevel! eq 0 (
            echo [成功] 代码已更新到最新版本
        ) else (
            echo [错误] 代码更新失败，请手动处理冲突
            pause
            exit /b 1
        )
    ) else (
        echo [信息] 代码已是最新版本
    )
)

:: ==================== 端口清理 ====================
echo.
echo [阶段3/4] 端口检测与清理...
echo.

:: 检查并杀死5000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do (
    echo [操作] 发现5000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
    if !errorlevel! equ 0 (
        echo [成功] PID %%a 已终止
    ) else (
        echo [信息] PID %%a 不存在或无法终止
    )
)
timeout /t 2 /nobreak >nul

:: 检查并杀死3000端口占用进程
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000') do (
    echo [操作] 发现3000端口占用，正在终止PID %%a...
    taskkill /pid %%a /f >nul 2>&1
    if !errorlevel! equ 0 (
        echo [成功] PID %%a 已终止
    ) else (
        echo [信息] PID %%a 不存在或无法终止
    )
)
timeout /t 2 /nobreak >nul

:: ==================== 服务启动 ====================
echo.
echo [阶段4/4] 启动服务...
echo.

:: 检查项目目录结构
if not exist "ai-tutor-system\aispring" (
    echo [错误] 未找到后端目录 ai-tutor-system\aispring
    pause
    exit /b 1
)

if not exist "ai-tutor-system\vue-app" (
    echo [错误] 未找到前端目录 ai-tutor-system\vue-app
    pause
    exit /b 1
)

:: 设置JVM参数
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

:: 设置Spring Boot参数
set "SPRING_OPTS=--server.port=5000 --logging.level.com.aispring=INFO --logging.file.name=logs/backend.log"

:: 启动后端服务
echo [启动] 后端服务 (Spring Boot)...
cd ai-tutor-system\aispring

:: 创建日志目录
if not exist "logs" mkdir logs

:: 启动Spring Boot应用
echo [启动] 执行: %MAVEN_CMD% spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" !SPRING_OPTS!
start "AI Study Backend - Port 5000" cmd /k "
title AI Study Backend - Port 5000 ^(Production Mode^)
color 0A
echo =========================================================================
echo        AI Study Backend 服务启动中...
echo        运行模式: Production
echo        服务端口: 5000
echo        启动时间: %date% %time%
echo        日志文件: logs/backend.log
echo        如需停止服务，请关闭此窗口
echo =========================================================================
echo.
echo JVM参数: !JVM_OPTS!
echo Spring参数: !SPRING_OPTS!
echo.
echo [注意] 首次启动可能需要较长时间，请耐心等待...
echo [注意] 等待出现 'Started .* in .* seconds' 表示启动成功
echo.
%MAVEN_CMD% spring-boot:run -Dspring-boot.run.jvmArguments="!JVM_OPTS!" !SPRING_OPTS!
echo.
echo [提示] 后端服务已停止
echo [提示] 按任意键退出...
pause >nul
"
cd ..

timeout /t 8 /nobreak >nul

:: 启动前端服务
echo [启动] 前端服务 (Vue.js)...
cd ai-tutor-system\vue-app

:: 检查并安装依赖
if not exist "node_modules" (
    echo [操作] 安装前端依赖...
    npm install --registry https://registry.npmmirror.com
    if !errorlevel! neq 0 (
        echo [错误] 前端依赖安装失败
        cd ..
        pause
        exit /b 1
    ) else (
        echo [成功] 前端依赖安装完成
    )
) else (
    echo [信息] 前端依赖已存在，跳过安装
)

:: 启动前端开发服务器
start "AI Study Frontend - Port 3000" cmd /k "
title AI Study Frontend - Port 3000 ^(Development Mode^)
color 0B
echo =========================================================================
echo        AI Study Frontend 服务启动中...
echo        运行模式: Development
echo        服务端口: 3000
echo        启动时间: %date% %time%
echo        如需停止服务，请关闭此窗口
echo =========================================================================
echo.
echo [注意] 前端服务启动中，请等待出现 'ready in .* ms' 表示启动成功
echo [注意] 访问 http://localhost:3000 查看前端界面
echo.
npm run dev
echo.
echo [提示] 前端服务已停止
echo [提示] 按任意键退出...
pause >nul
"
cd ..

echo.
echo =========================================================================
echo 服务启动完成！
echo.
echo 当前时间: %date% %time%
echo.
echo 后端服务: http://localhost:5000
echo API文档: http://localhost:5000/swagger-ui.html
echo 管理后台: http://localhost:5000/admin
echo.
echo 前端访问: http://localhost:3000
echo.
echo 服务窗口:
echo   - 后端: AI Study Backend - Port 5000
echo   - 前端: AI Study Frontend - Port 3000
echo.
echo 操作提示:
echo   - 关闭对应窗口可停止相应服务
echo   - 服务日志可在对应窗口查看
echo   - 首次启动可能需要等待30-60秒
echo =========================================================================
echo.

pause
