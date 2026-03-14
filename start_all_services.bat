@echo off
setlocal enabledelayedexpansion

:: Set console to UTF-8
chcp 65001 >nul

echo.
echo ========================================================
echo           AI Study Project - Start All Services
echo ========================================================
echo.

:: Save the root directory
set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

echo [1/5] Verifying dependencies...

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java and add to PATH.
    pause
    exit /b 1
) else (
    echo [SUCCESS] Java found.
)

:: Check Node.js
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install Node.js and add to PATH.
    pause
    exit /b 1
) else (
    echo [SUCCESS] Node.js found.
)

echo.
echo [2/5] Starting Backend (Spring Boot)...

cd /d "%ROOT_DIR%"
if not exist "ai-tutor-system\aispring" (
    echo [ERROR] Directory ai-tutor-system\aispring not found.
    pause
    exit /b 1
)

:: Set JVM Opts
set "JVM_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Dfile.encoding=UTF-8"

:: Start Backend
cd ai-tutor-system\aispring
start "AI-Backend-5000" cmd /k "title AI Study Backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments=%JVM_OPTS% && pause"

timeout /t 5 /nobreak >nul

echo.
echo [3/5] Starting Frontend (Nuxt/Vue)...

cd /d "%ROOT_DIR%"
if not exist "ai-tutor-system\apps\client" (
    echo [ERROR] Directory ai-tutor-system\apps\client not found.
    pause
    exit /b 1
)

:: Start Frontend
cd ai-tutor-system\apps\client
start "AI-Frontend-3000" cmd /k "title AI Study Frontend && npm run dev && pause"

timeout /t 3 /nobreak >nul

echo.
echo [4/5] Starting Word Game...

cd /d "%ROOT_DIR%"
if not exist "ai-tutor-system\aispring\word-game" (
    echo [ERROR] Directory ai-tutor-system\aispring\word-game not found.
    pause
    exit /b 1
)

:: Start Word Game
cd ai-tutor-system\aispring\word-game
start "Word-Game-5200" cmd /k "title Word Game && npm run dev && pause"

timeout /t 3 /nobreak >nul

echo.
echo [5/5] Starting CV Site...

cd /d "%ROOT_DIR%"
if not exist "cv-site" (
    echo [ERROR] Directory cv-site not found.
    pause
    exit /b 1
)

:: Check Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Python not found. Skipping CV Site.
) else (
    cd cv-site
    start "CV-Site-3100" cmd /k "title CV Site && python -m http.server 3100 && pause"
)

echo.
echo ========================================================
echo All services started!
echo.
echo Access addresses:
echo   Backend API: http://localhost:5000/swagger-ui.html
echo   Frontend App: http://localhost:3000
echo   Word Game: http://localhost:5200
echo   CV Site: http://localhost:3100
echo.
echo Note: Keep the windows open to keep services running.
echo ========================================================
echo.

timeout /t 3 /nobreak >nul
