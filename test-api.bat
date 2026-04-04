@REM Quick test script for PayAssure OTP endpoint
@REM Prerequisites: Backend running on http://localhost:8080

@echo off
echo Testing PayAssure OTP Endpoint...
echo.

REM Test 1: Send OTP
echo [TEST 1] Sending OTP to phone 9876543210...
powershell -Command "
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/otp/send' `
        -Method 'POST' `
        -Headers @{'Content-Type'='application/json'} `
        -Body '{\"phone\":\"9876543210\"}' `
        -UseBasicParsing
    Write-Host 'Status:' $response.StatusCode
    Write-Host 'Response:' $response.Content
} catch {
    Write-Host 'ERROR:' $_.Exception.Message -ForegroundColor Red
}
"

echo.
echo [TEST 2] Verify OTP 123456...
powershell -Command "
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/otp/verify' `
        -Method 'POST' `
        -Headers @{'Content-Type'='application/json'} `
        -Body '{\"phone\":\"9876543210\",\"otp\":\"123456\"}' `
        -UseBasicParsing
    Write-Host 'Status:' $response.StatusCode
    Write-Host 'Response:' $response.Content
} catch {
    Write-Host 'ERROR:' $_.Exception.Message -ForegroundColor Red
}
"

echo.
echo [TEST 3] Get Available Zones...
powershell -Command "
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/zones' `
        -Method 'GET' `
        -Headers @{'Content-Type'='application/json'} `
        -UseBasicParsing
    Write-Host 'Status:' $response.StatusCode
    if ($response.Content) { Write-Host 'Found' ([regex]::Matches($response.Content, 'id').Count) 'zones' }
} catch {
    Write-Host 'ERROR:' $_.Exception.Message -ForegroundColor Red
}
"

echo.
echo Test complete!
