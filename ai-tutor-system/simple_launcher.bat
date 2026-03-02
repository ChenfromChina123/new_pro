@echo off
setlocal enabledelayedexpansion

chcp 65001 >nul

echo.
echo =========================================================================
echo                    AI Study Project - Full Stack Launcher
echo =========================================================================
echo.

cd /d "%~dp0"

echo [Time] %date% %time%
echo.

:: Environment Check
echo [Stage 1/4] Environment Check...
echo.

where git >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Git not found
    pause
    exit /b 1
) else (
    echo [OK] Git found
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    pause
    exit /b 1
) else (
    echo [OK] Java found
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Maven not found, using mvnw
    set MAVEN_CMD=mvnw
) else (
    echo [OK] Maven found
    set MAVEN_CMD=mvn
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found
    pause
    exit /b 1
) else (
    echo [OK] Node.js found
)

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm not found
    pause
    exit /b 1
) else (
    echo [OK] npm found
)

:: Git Update
echo.
echo [Stage 2/4] Git Update...
echo.

if not exist ".git" (
    echo [ERROR] Not a git repository
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('git branch ^| findstr "^*"') do set "current_branch=%%i"
set "current_branch=!current_branch:* =!"

echo [INFO] Current branch: !current_branch!

echo [ACTION] Fetching updates...
git fetch origin
if !errorlevel! equ 0 (
    for /f %%i in ('git rev-list HEAD...origin/!current_branch! --count 2^^^>nul') do set "update_count=%%i"
    if !update_count! gtr 0 (
        echo [ACTION] Merging !update_count! updates...
        git merge origin/!current_branch!
        if !errorlevel! equ 0 (
            echo [OK] Code updated
        ) else (
            echo [ERROR] Merge failed
            pause
            exit /b 1
        )
    ) else (
        echo [INFO] Code is up to date
    )
) else (
    echo [WARN] Cannot fetch updates
)

:: Port Detection and Cleanup
echo.
echo [Stage 3/4] Port Detection and Cleanup...
echo.

:: Check and kill port 5000
echo [CHECK] Port 5000...
set "port_5000_found=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000.*LISTENING"') do (
    set "port_5000_found=1"
    set "pid_5000=%%a"
    echo [FOUND] Port 5000 occupied by PID !pid_5000!
    echo [ACTION] Killing process !pid_5000!...
    taskkill /pid !pid_5000! /f >nul 2>&1
    if !errorlevel! equ 0 (
        echo [OK] Process !pid_5000! killed
    ) else (
        echo [WARN] Cannot kill process !pid_5000!
    )
)

if !port_5000_found! equ 0 (
    echo [INFO] Port 5000 is free
)

if !port_5000_found! equ 1 (
    echo [WAIT] Waiting for port 5000 to be released...
    timeout /t 3 /nobreak >nul
)

:: Check and kill port 3000
echo.
echo [CHECK] Port 3000...
set "port_3000_found=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000.*LISTENING"') do (
    set "port_3000_found=1"
    set "pid_3000=%%a"
    echo [FOUND] Port 3000 occupied by PID !pid_3000!
    echo [ACTION] Killing process !pid_3000!...
    taskkill /pid !pid_3000! /f >nul 2>&1
    if !errorlevel! equ 0 (
        echo [OK] Process !pid_3000! killed
    ) else (
        echo [WARN] Cannot kill process !pid_3000!
    )
)

if !port_3000_found! equ 0 (
    echo [INFO] Port 3000 is free
)

if !port_3000_found! equ 1 (
    echo [WAIT] Waiting for port 3000 to be released...
    timeout /t 3 /nobreak >nul
)

:: Start Services
echo.
echo [Stage 4/4] Starting Services...
echo.

if not exist "ai-tutor-system\aispring" (
    echo [ERROR] Backend directory not found
    pause
    exit /b 1
)

if not exist "ai-tutor-system\vue-app" (
    echo [ERROR] Frontend directory not found
    pause
    exit /b 1
)

set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8"

echo [START] Backend Service (Spring Boot)...
cd ai-tutor-system\aispring

if not exist "logs" mkdir logs

start "AI Study Backend - Port 5000" cmd /k "title AI Study Backend - Port 5000 && color 0A && echo Backend starting on port 5000... && !MAVEN_CMD! spring-boot:run -Dspring-boot.run.jvmArguments=\"!JVM_OPTS!\" --server.port=5000 --logging.level.com.aispring=INFO --logging.file.name=logs/backend.log"

cd ..

echo [WAIT] Waiting for backend to start...
timeout /t 10 /nobreak >nul

echo [START] Frontend Service (Vue.js)...
cd ai-tutor-system\vue-app

if not exist "node_modules" (
    echo [ACTION] Installing frontend dependencies...
    npm install --registry https://registry.npmmirror.com
    if !errorlevel! neq 0 (
        echo [ERROR] Frontend dependency installation failed
        cd ..
        pause
        exit /b 1
    )
)

start "AI Study Frontend - Port 3000" cmd /k "title AI Study Frontend - Port 3000 && color 0B && echo Frontend starting on port 3000... && npm run dev"

cd ..

echo.
echo =========================================================================
echo Services Started Successfully!
echo.
echo [TIME] %date% %time%
echo.
echo [BACKEND] http://localhost:5000
echo [API DOC] http://localhost:5000/swagger-ui.html
echo [ADMIN]   http://localhost:5000/admin
echo.
echo [FRONTEND] http://localhost:3000
echo.
echo [WINDOWS]
echo   - Backend: AI Study Backend - Port 5000
echo   - Frontend: AI Study Frontend - Port 3000
echo.
echo [NOTES]
echo   - Close the window to stop the service
echo   - First startup may take 30-60 seconds
echo =========================================================================
echo.

pause
