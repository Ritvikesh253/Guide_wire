# PayAssure API Testing Guide

Complete guide to testing all 5 features of the PayAssure platform.

## Table of Contents
1. [Feature 1: Zone Selection](#feature-1-zone-selection)
2. [Feature 2: ML Premium Calculation](#feature-2-ml-premium-calculation)
3. [Feature 3: Parametric Automation](#feature-3-parametric-automation)
4. [Feature 4: Instant Payouts](#feature-4-instant-payouts)
5. [Feature 5: Aadhaar Validation](#feature-5-aadhaar-validation)
6. [Full Registration Flow](#full-registration-flow)
7. [Docker Testing](#docker-testing)

---

## Feature 1: Zone Selection

**Description**: Interactive map with Leaflet.js + OpenStreetMap. Displays 8 pre-configured Chennai zones with risk indices for geofencing.

### 1.1 Get All Available Zones
```bash
curl -X GET http://localhost:8080/api/zones \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
[
  {
    "id": 1,
    "name": "Central Business District",
    "city": "Chennai",
    "centerLat": 13.0549,
    "centerLon": 80.2821,
    "riskIndex": 0.7,
    "geojson": "POLYGON((...))",
    "basePremium": 500
  },
  {
    "id": 2,
    "name": "Harbour Area",
    "city": "Chennai",
    "centerLat": 13.1347,
    "centerLon": 80.2829,
    "riskIndex": 1.2,
    "geojson": "POLYGON((...))",
    "basePremium": 600
  }
  // ... 6 more zones
]
```

### 1.2 Get Zone Details by ID
```bash
curl -X GET http://localhost:8080/api/zones/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Use Case**: Worker selects zone on Step 3 (Leaflet map component shows this data)

---

## Feature 2: ML Premium Calculation

**Description**: Dynamic premium calculation using elevation, weather, flood history → Scikit-Learn Random Forest → multiplier (0.8x to 1.5x).

### 2.1 Calculate Premium
Calculate the final premium based on worker location and zone.

```bash
curl -X POST "http://localhost:8080/api/premium/calculate?zoneId=1&lat=13.0827&lon=80.2707&basePrice=500" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Parameters**:
- `zoneId`: Zone ID (1-8)
- `lat`: Worker latitude
- `lon`: Worker longitude
- `basePrice`: Base premium in ₹ (default 500-1000)

**Response**:
```json
{
  "zoneId": 1,
  "basePremium": 500,
  "elevationRisk": 1.1,
  "weatherRisk": 1.0,
  "floodHistoryRisk": 0.9,
  "mlMultiplier": 1.25,
  "finalPremium": 562.5,
  "message": "Premium calculated using ML Random Forest model"
}
```

### 2.2 ML Service Details
- **Service**: `ml-service` (Flask, Python 3.9)
- **Port**: 5000
- **Endpoint**: `POST /api/predict-premium`
- **Algorithm**: Scikit-Learn Random Forest (10 trees, max_depth=5)
- **Training Data**: 5000 synthetic samples with elevation/weather/flood characteristics

---

## Feature 3: Parametric Automation

**Description**: Every 60 seconds, polls Mock Disruption API. If rain > 70mm or strike = TRUE, auto-creates claim and triggers payout.

### 3.1 Trigger Disruption Event (Manual Testing)
Manually create a disruption scenario for testing.

```bash
curl -X GET "http://localhost:8080/api/claims/trigger-disruption?zone=Central%20Business%20District&rainMm=85&status=RED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Parameters**:
- `zone`: Zone name
- `rainMm`: Rainfall in mm (>70 triggers claim)
- `status`: Weather status (RED/ORANGE/GREEN)

**Response**:
```json
{
  "success": true,
  "claimId": 42,
  "message": "Parametric claim auto-created due to disruption",
  "claimAmount": 500,
  "payoutStatus": "INITIATED"
}
```

### 3.2 Automatic Polling (Behind the Scenes)
The backend runs a scheduled task every 60 seconds:

```bash
# No manual endpoint needed — this is automatic in ParametricAutomationService
# Logs show:
# [2024-01-15 10:23:45] Checking disruption events from mock-api:8090...
# [2024-01-15 10:23:45] Rain in Zone 5: 85mm > 70mm threshold → Claim #42 created
# [2024-01-15 10:23:46] Initiating payout for Claim #42: ₹500 to UPI
```

### 3.3 Mock Disruption API
- **Service**: `mock-api` (Flask)
- **Port**: 8090
- **Endpoint**: `GET /api/disruptionEvents`
- **Returns**: Random disruption scenarios (rain 0-150mm, strike TRUE/FALSE)

---

## Feature 4: Instant Payouts

**Description**: RazorpayX UPI payouts. Worker provides UPI ID → Razorpay creates contact + fund account → UPI transfer initiated.

### 4.1 Initiate Payout
```bash
curl -X POST "http://localhost:8080/api/payouts/initiate?upiId=arun.kumar@upi&amount=500&claimId=42" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Parameters**:
- `upiId`: UPI ID (e.g., `name@okhdfcbank`, `name@googleplay`)
- `amount`: Amount in ₹
- `claimId`: Associated claim ID

**Response**:
```json
{
  "payoutId": 101,
  "claimId": 42,
  "upiId": "arun.kumar@upi",
  "amount": 500,
  "currency": "INR",
  "status": "INITIATED",
  "razorpayPayoutId": "pout_ABC123XYZ",
  "razorpayContactId": "cont_ABC123XYZ",
  "razorpayFundAccountId": "fa_ABC123XYZ",
  "initiatedAt": "2024-01-15T10:30:00Z",
  "message": "Payout initiated successfully"
}
```

### 4.2 Check Payout Status
```bash
curl -X GET http://localhost:8080/api/payouts/101/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "payoutId": 101,
  "claimId": 42,
  "upiId": "arun.kumar@upi",
  "amount": 500,
  "status": "SETTLED",
  "razorpayPayoutId": "pout_ABC123XYZ",
  "bankTransferReference": "NEFT20240115ABCD1234",
  "settledAt": "2024-01-15T10:35:00Z",
  "message": "Payout settled to UPI"
}
```

**Payout Status Values**:
- `INITIATED`: Payout processing started
- `PENDING`: Waiting for bank confirmation
- `PROCESSING`: In-flight to bank
- `SETTLED`: Successfully transferred
- `FAILED`: Failed (retry possible)
- `REVERSED`: Reversed/refunded

### 4.3 Razorpay Configuration
- **Mode**: Test mode (uses mock data in sandbox)
- **Sandbox UPI IDs** for testing:
  - `success@upi` → Success
  - `fail@upi` → Failure
  - `random@upi` → Random result

---

## Feature 5: Aadhaar Validation

**Description**: Validates Aadhaar, fetches e-KYC data (name, DOB, gender, address, photo), and verifies face liveness against selfie.

### 5.1 Request OTP from UIDAI
```bash
curl -X POST http://localhost:8080/api/aadhaar/otp \
  -H "Content-Type: application/json" \
  -d '{
    "aadhaarNumber": "999941057058"
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "OTP sent to registered mobile"
}
```

**Sandbox Test Aadhaar Numbers** (UIDAI provided):
- `999941057058` → Male, full KYC data
- `999971658847` → Female, full KYC data
- Any 12-digit number → Mock response (for sandbox without UIDAI access)

### 5.2 Verify Aadhaar & Fetch e-KYC
```bash
curl -X POST "http://localhost:8080/api/aadhaar/verify?aadhaarNumber=999941057058&otp=123456" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "verified": true,
  "legalName": "Arun Kumar Singh",
  "dob": "15/01/1990",
  "gender": "M",
  "address": "123 Main Street, Chennai, Tamil Nadu 600001",
  "photoBase64": "iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAY...[truncated]",
  "message": "Aadhaar e-KYC successful"
}
```

### 5.3 Verify Face Liveness
Compare selfie against Aadhaar photo to ensure real person (not a printed photo).

```bash
curl -X POST "http://localhost:8080/api/aadhaar/verify-face?selfieBase64=SELFIE_BASE64&referencePhotoBase64=AADHAAR_PHOTO_BASE64" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Face Matching Providers**:
- **mock**: Always returns 95% confidence (for sandbox)
- **aws-rekognition**: AWS Rekognition CompareFaces API (production)
- **google-vision**: Google Cloud Vision API
- **local-opencv**: Python OpenCV ML model (self-hosted)

**Response**:
```json
{
  "match": true,
  "confidence": 0.96,
  "message": "Face match successful — 96% confidence"
}
```

**Confidence Thresholds**:
- `>= 0.85`: Match accepted, worker approved
- `0.70-0.85`: Manual review required
- `< 0.70`: Match rejected, request re-verification

---

## Full Registration Flow

Complete 4-step registration showcasing all 5 features:

### Step 1: Basic Info + Phone OTP
```bash
# User enters phone number
curl -X POST http://localhost:8080/api/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210"}'

# Verify OTP
curl -X POST http://localhost:8080/api/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210", "otp": "123456"}'

# Get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210", "password": "temp_otp_password"}'
```

### Step 2: Aadhaar Validation (Feature 5)
```bash
# Request Aadhaar OTP
curl -X POST http://localhost:8080/api/aadhaar/otp \
  -H "Content-Type: application/json" \
  -d '{"aadhaarNumber": "999941057058"}'

# Fetch e-KYC
curl -X POST "http://localhost:8080/api/aadhaar/verify?aadhaarNumber=999941057058&otp=123456" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Verify Face (Feature 5)
curl -X POST "http://localhost:8080/api/aadhaar/verify-face?selfieBase64=$SELFIE_B64&referencePhotoBase64=$AADHAAR_PHOTO_B64" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Step 3: Zone Selection (Feature 1)
```bash
# Get available zones
curl -X GET http://localhost:8080/api/zones \
  -H "Authorization: Bearer $JWT_TOKEN"

# Select zone (ID 1 = Central Business District)
# Frontend sends: zoneId=1, lat=13.0827, lon=80.2707
```

### Step 4: Premium Calculation (Feature 2)
```bash
# Calculate premium based on zone & location
curl -X POST "http://localhost:8080/api/premium/calculate?zoneId=1&lat=13.0827&lon=80.2707&basePrice=500" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Response shows: finalPremium = 562.5 (base 500 × ML multiplier 1.125)
```

### After Registration: Parametric Automation (Feature 3) & Payouts (Feature 4)
```bash
# Every 60 seconds, backend checks for disruptions:
# IF rain > 70mm:
#   → Auto-create claim (₹500)
#   → Initiate payout to worker's UPI
#   → Worker receives ₹500 within 2-3 minutes

curl -X GET http://localhost:8080/api/payouts/101/status \
  -H "Authorization: Bearer $JWT_TOKEN"
# Status: SETTLED → Worker's account is credited
```

---

## Docker Testing

### Starting the Full Stack
```bash
cd ~/payassure

# Copy .env with your credentials
cp .env.example .env
# Edit .env with real Razorpay/Setu/Weather API keys

# Build all images
docker-compose build

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps
# Expected: postgres, mysql, backend, ml-service, mock-api, frontend all "Up"
```

### Testing Inside Docker
```bash
# Backend service logs
docker-compose logs -f backend

# Test Zone API
curl -X GET http://localhost:8080/api/zones

# Test ML service
docker-compose exec ml-service curl -X POST http://localhost:5000/api/predict-premium \
  -H "Content-Type: application/json" \
  -d '{"elevation_risk": 1.0, "weather_risk": 1.0, "flood_history_risk": 1.0}'

# Test Mock API
curl -X GET http://localhost:8090/api/disruptionEvents

# View MySQL
docker-compose exec mysql mysql -u payassure_user -p payassure_db \
  -e "SELECT * FROM zones LIMIT 3;"
```

### Stopping Services
```bash
docker-compose down
docker volume rm payassure_mysql_data  # Optional: clean database
```

---

## Performance Testing

### Load Testing Premium Calculation (1000 requests)
```bash
# Using Apache Bench
ab -n 1000 -c 10 "http://localhost:8080/api/premium/calculate?zoneId=1&lat=13.0827&lon=80.2707&basePrice=500"

# Expected: ~500-1000 req/sec depending on system
```

### ML Service Response Time
```bash
for i in {1..100}; do
  time curl -X POST http://localhost:5000/api/predict-premium \
    -H "Content-Type: application/json" \
    -d '{"elevation_risk": 1.0, "weather_risk": 1.0, "flood_history_risk": 1.0}'
done | grep real | awk '{sum+=$2; print "Avg: " sum/100}'
```

---

## Debugging

### Check Backend Logs
```bash
tail -f backend/logs/payassure.log
```

### Enable Debug Mode
Set in `application.yml`:
```yaml
logging:
  level:
    com.payassure: DEBUG
    com.payassure.service.ParametricAutomationService: DEBUG
```

### Database Schema
```sql
-- Check zones
SELECT * FROM zones;

-- Check claims created via parametric automation
SELECT * FROM claims WHERE auto_triggered = 1;

-- Check payouts
SELECT * FROM payout_receipts WHERE status = 'SETTLED';
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Connection refused: localhost:8080` | Ensure backend is running: `docker-compose logs backend` |
| `JWT token expired` | Get new token: `curl -X POST /api/auth/login` |
| `Aadhaar OTP failed` | Check `.env` for valid Setu credentials |
| `Razorpay payout pending` | Use sandbox UPI: `success@upi` or `random@upi` |
| `ML service timeout` | Check if ml-service docker is running: `docker-compose logs ml-service` |
| `Zone query error` | Verify MySQL spatial index: `SELECT *, ST_AsGeoJSON(geojson) FROM zones LIMIT 1;` |

---

## Next Steps

1. **Deploy to Production**: Update `.env` with real API credentials
2. **Load Testing**: Use Apache JMeter for sustained load (1000+ concurrent workers)
3. **API Documentation**: Generate Swagger/OpenAPI from controller annotations
4. **Monitoring**: Set up ELK stack (Elasticsearch, Logstash, Kibana) for logs
5. **CI/CD**: GitHub Actions to auto-build Docker images on push

---

*Last Updated: January 15, 2024*
*PayAssure v1.0.0 — Parametric Income Insurance for Gig Workers*
