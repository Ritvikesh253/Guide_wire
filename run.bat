@echo off
REM PayAssure Project - Quick Start Script (Windows CMD)
REM This script starts all PayAssure services

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              PayAssure - Quick Start (Windows)                ║
echo ║         Press Ctrl+C in each window to stop services         ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

setlocal enabledelayedexpansion

REM Check if MySQL is running
echo Checking MySQL...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if errorlevel 1 (
    echo WARNING: MySQL not detected. Services will fail without database.
    echo Please start MySQL before proceeding.
    pause
)

REM Start Backend (Java)
echo.
echo Starting Backend (Java Spring Boot on port 8080)...
cd /d "%~dp0backend"
start "PayAssure Backend" cmd /k "java -jar target/payassure-backend-1.0.0.jar"
timeout /t 2 /nobreak

REM Start ML Service (Python)
echo Starting ML Service (Python Flask on port 5000)...
cd /d "%~dp0backend\ml-service"
start "PayAssure ML Service" cmd /k "python -m pip install -q -r requirements.txt && python app.py"
timeout /t 2 /nobreak

REM Start Mock API (Python)
echo Starting Mock Disruption API (Python Flask on port 8090)...
cd /d "%~dp0backend\mock-disruption-api"
start "PayAssure Mock API" cmd /k "python -m pip install -q flask gunicorn python-dotenv && python app.py"
timeout /t 2 /nobreak

REM Start Frontend (React)
echo Starting Frontend (React on port 3000)...
cd /d "%~dp0frontend"
start "PayAssure Frontend" cmd /k "npm install && npm start"

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║           Services Starting... (4 windows opened)             ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo Open in browser: http://localhost:3000
echo API Base: http://localhost:8080/api
echo.
echo Each service runs in its own window. Close windows to stop services.
echo.

cd /d "%~dp0"

pause
