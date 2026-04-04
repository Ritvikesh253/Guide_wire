#!/usr/bin/env pwsh
# PayAssure Project Startup Script
# Starts all services: Backend (Java), ML Service (Python), Mock API, and Frontend (React)
# Prerequisites: Java 17, Python 3.9+, Node.js, MySQL 8.0

param(
    [string]$Service = "all"
)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptRoot

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║              PayAssure Project - Service Startup              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

# Function to check if a port is in use
function Test-Port {
    param([int]$Port)
    $exists = netstat -ano | findstr ":$Port"
    return $null -ne $exists
}

# Function to start a service
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$Command,
        [int]$Port
    )
    
    if (Test-Port -Port $Port) {
        Write-Host "❌ Port $Port already in use (Service: $ServiceName)" -ForegroundColor Red
        return $false
    }
    
    Write-Host "`n▶ Starting $ServiceName on port $Port..." -ForegroundColor Yellow
    Invoke-Expression $Command
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $ServiceName started successfully" - ForegroundColor Green
        return $true
    }
    else {
        Write-Host "✗ Failed to start $ServiceName" -ForegroundColor Red
        return $false
    }
}

# Determine which service(s) to start
$services = @()
switch ($Service) {
    "all" {
        $services = @("backend", "ml", "mock-api", "frontend")
    }
    "backend" {
        $services = @("backend")
    }
    "ml" {
        $services = @("ml")
    }
    "mock-api" {
        $services = @("mock-api")
    }
    "frontend" {
        $services = @("frontend")
    }
    default {
        Write-Host "Usage: .\startup.ps1 [all|backend|ml|mock-api|frontend]" -ForegroundColor Yellow
        exit 1
    }
}

# Start MySQL (assuming it's already  installed as a Windows service)
Write-Host "`n▶ Checking MySQL..." -ForegroundColor Yellow
$mysqlStatus = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if ($mysqlStatus -and $mysqlStatus.Status -ne "Running") {
    Write-Host "  Starting MySQL..." -ForegroundColor Gray
    Start-Service -Name "MySQL80" -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}

# Backend - Java Spring Boot
if ($services -contains "backend") {
    Write-Host "`n▶ Building backend..." -ForegroundColor Yellow
    Set-Location "$scriptRoot\backend"
    mvn clean package -DskipTests -q
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Failed to build backend" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✓ Backend build successful" -ForegroundColor Green
    Write-Host "▶ Starting backend JAR..." -ForegroundColor Yellow
    $backendJob = Start-Job -ScriptBlock {
        param($path)
        Set-Location $path
        java -jar target/payassure-backend-1.0.0.jar
    } -ArgumentList $scriptRoot\backend
    
    Write-Host "✓ Backend service started (Job ID: $($backendJob.Id))" -ForegroundColor Green
}

# ML Service - Python Flask
if ($services -contains "ml") {
    Write-Host "`n▶ Starting ML Service..." -ForegroundColor Yellow
    $mlJob = Start-Job -ScriptBlock {
        param($path)
        Set-Location "$path\backend\ml-service"
        python -m pip install -r requirements.txt -q
        python app.py
    } -ArgumentList $scriptRoot
    
    Write-Host "✓ ML Service started (Job ID: $($mlJob.Id))" -ForegroundColor Green
    Write-Host "  Listening on: http://localhost:5000" -ForegroundColor Gray
}

# Mock Disruption API - Python Flask
if ($services -contains "mock-api") {
    Write-Host "`n▶ Starting Mock Disruption API..." -ForegroundColor Yellow
    $mockJob = Start-Job -ScriptBlock {
        param($path)
        Set-Location "$path\backend\mock-disruption-api"
        python -m pip install flask gunicorn python-dotenv -q
        python app.py
    } -ArgumentList $scriptRoot
    
    Write-Host "✓ Mock API started (Job ID: $($mockJob.Id))" -ForegroundColor Green
    Write-Host "  Listening on: http://localhost:8090" -ForegroundColor Gray
}

# Frontend - React
if ($services -contains "frontend") {
    Write-Host "`n▶ Installing frontend dependencies..." -ForegroundColor Yellow
    Set-Location "$scriptRoot\frontend"
    npm install -q --legacy-peer-deps
    
    Write-Host "✓ Frontend dependencies installed" -ForegroundColor Green
    Write-Host "▶ Starting React frontend..." -ForegroundColor Yellow
    $frontendJob = Start-Job -ScriptBlock {
        param($path)
        Set-Location "$path\frontend"
        npm start
    } -ArgumentList $scriptRoot
    
    Write-Host "✓ Frontend started (Job ID: $($frontendJob.Id))" -ForegroundColor Green
    Write-Host "  Listening on: http://localhost:3000" -ForegroundColor Gray
}

# Summary
Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║            ✓ All PayAssure services started!                  ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Green

Write-Host `
@"

📱 Frontend:       http://localhost:3000
🔗 Backend API:    http://localhost:8080/api
🤖 ML Service:     http://localhost:5000
📡 Mock API:       http://localhost:8090
💾 MySQL:          localhost:3306

Enter the following in the OTP field during testing: 123456
Default credentials:
  - Phone: 9876543210
  - Aadhaar: 999941057058 (UIDAI test account)

Run 'Get-Job | Stop-Job' to stop all background services.

"@ -ForegroundColor Cyan

# Keep script running
Write-Host "Services running. Press Ctrl+C to exit.`n" -ForegroundColor Yellow
while ($true) { Start-Sleep -Seconds 1 }
