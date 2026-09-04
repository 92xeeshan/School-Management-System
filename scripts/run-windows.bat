@echo off
REM =====================================================================
REM School Management System - Run UI + Backend (Windows)
REM =====================================================================
REM Usage:
REM     Double-click scripts\run-windows.bat
REM     or run it from a Command Prompt.
REM
REM Requirements (see docs\README-BACKEND.md and docs\README-FRONTEND.md):
REM     JDK 21, Maven 3.9+, Node.js 20+, PostgreSQL 15
REM     PostgreSQL must already be running.
REM =====================================================================
setlocal

REM Load optional .env file (database credentials etc.)
if exist "%~dp0..\.env" (
    echo [run] Loading .env
    for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0..\.env") do set "%%a=%%b"
)

REM Prerequisite checks
where java >nul 2>nul || (echo [run] ERROR: java not found. Install JDK 21 and add it to PATH. & goto :error)
where mvn  >nul 2>nul || (echo [run] ERROR: mvn not found. Install Maven 3.9+ and add it to PATH. & goto :error)
where node >nul 2>nul || (echo [run] ERROR: node not found. Install Node.js 20+. & goto :error)
where npm  >nul 2>nul || (echo [run] ERROR: npm not found. Install Node.js 20+. & goto :error)

REM Install frontend dependencies on first run
if not exist "%~dp0..\frontend\node_modules" (
    echo [run] Installing frontend dependencies...
    pushd "%~dp0..\frontend"
    call npm install
    popd
)

echo [run] Starting backend on http://localhost:8080
start "SchoolMS Backend" cmd /k "cd /d %~dp0..\backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"

echo [run] Starting UI on http://localhost:4200
start "SchoolMS Frontend" cmd /k "cd /d %~dp0..\frontend && npm start"

echo.
echo ================================================================
echo   School Management System is starting
echo   UI:      http://localhost:4200
echo   API:     http://localhost:8080
echo   Swagger: http://localhost:8080/swagger-ui.html
echo   Demo login (any of these / password Admin@123):
echo     superadmin ^| admin ^| teacher ^| parent ^| student
echo   Close the two console windows to stop the servers.
echo ================================================================
echo.
echo NOTE: give the servers ~30-60 seconds to start, then open the UI.
goto :end

:error
echo.
echo Setup failed. See docs\README-BACKEND.md and docs\README-FRONTEND.md for instructions.
exit /b 1

:end
endlocal
