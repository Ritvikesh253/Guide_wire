# PayAssure — Complete Setup & Running Guide

PayAssure is a parametric income insurance platform for gig workers with 5 advanced features:
1. **Zone Selection** with geospatial mapping (Leaflet.js)
2. **ML-Driven Dynamic Premium** calculation
3. **Parametric Automation** with real-time monitoring
4. **Instant Payout Processing** via RazorpayX
5. **Aadhaar ID Matching & Real Selfie Validation**

---

## Prerequisites

Ensure you have the following installed:

### Java & Maven
```bash
java -version              # Should be 17+
mvn -version              # Should be 3.8+
```

### Node.js & npm
```bash
node --version            # Should be 18+
npm --version
```

### MySQL 8.0+
```bash
mysql --version
mysql -u root -p
```

### Python 3.9+ (for ML service)
```bash
python --version
pip --version
```

---

## Part 1: Database Setup

### Step 1A: Create PayAssure Database

```bash
# Option 1: Using provided SQL script
mysql -u root -p < mysql-setup-enhanced.sql

# Option 2: Manual commands
mysql -u root -p -e "
CREATE DATABASE IF NOT EXISTS payassure CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payassure;
-- Then run the SQL from mysql-setup-enhanced.sql
"
```

### Step 1B: Verify Database Schema

```bash
mysql -u root -p payassure -e "SHOW TABLES;"
```

You should see:
- workers
- otp_records
- zones
- active_policies
- claims
- payout_receipts
- disruption_events
- flood_history

---

## Part 2: Backend Setup (Spring Boot)

### Step 2A: Configure Database Connection

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payassure?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
    username: root                    # Change to your MySQL user
    password: extinction@7            # Change to your MySQL password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update               # Auto-create tables
```

### Step 2B: Configure API Keys

Update `backend/src/main/resources/application.yml` with your API keys:

```yaml
payassure:
  # Setu Aadhaar API (get from https://setu.co)
  setu:
    base-url: https://dg-sandbox.setu.co  # Sandbox for testing
    client-id: ${SETU_CLIENT_ID}
    client-secret: ${SETU_CLIENT_SECRET}

  # Razorpay (get from https://razorpay.com)
  razorpay:
    key-id: ${RAZORPAY_KEY_ID}
    key-secret: ${RAZORPAY_KEY_SECRET}

  # OpenWeather API (get from https://openweathermap.org)
  openweather:
    api-key: ${OPENWEATHER_API_KEY}

  # Python ML Service
  python:
    ml-service-url: http://localhost:5000

  # JWT Configuration
  jwt:
    secret: payassure-super-secret-key-minimum-32-chars-change-in-prod-2024
    expiry-ms: 86400000              # 24 hours
```

### Step 2C: Build & Run Backend

```bash
cd backend

# Build
mvn clean package -DskipTests

# Run
java -jar target/payassure-backend-1.0.0.jar

# Or run Maven directly
mvn spring-boot:run
```

Backend will start at: `http://localhost:8080`

---

## Part 3: Frontend Setup (React)

### Step 3A: Install Dependencies

```bash
cd frontend
npm install

# Include Leaflet.js for zone mapping
npm install leaflet react-leaflet
```

### Step 3B: Configure API Base URL

Edit `frontend/src/services/api.js`:

```javascript
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

// Set environment variable
export const configure = () => {
  if (process.env.NODE_ENV === 'development') {
    // API_BASE_URL will use http://localhost:8080/api
  }
};
```

### Step 3C: Run Frontend

```bash
cd frontend

# Development mode
npm start

# Runs at: http://localhost:3000
```

---

## Part 4: Python ML Service Setup (Optional but Recommended)

### Step 4A: Create Python Virtual Environment

```bash
cd backend/ml-service

python -m venv venv

# Activate
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate
```

### Step 4B: Install Python Dependencies

```bash
pip install flask scikit-learn pandas numpy requests python-dotenv gunicorn

# Create requirements.txt
pip freeze > requirements.txt
```

### Step 4C: Create ML Service

Create `backend/ml-service/app.py`:

```python
from flask import Flask, request, jsonify
from sklearn.ensemble import RandomForestRegressor
import numpy as np
import json
import os

app = Flask(__name__)
loaded_model = None

def train_model():
    """Train Random Forest model for premium calculation"""
    # Training data: [elevation_risk, weather_risk, flood_history_risk] -> premium_multiplier
    X_train = np.array([
        [0.8, 0.7, 0.9],
        [1.2, 1.0, 1.1],
        [1.5, 1.4, 1.3],
        [0.7, 0.8, 0.8],
        [1.3, 1.2, 1.2],
    ])
    y_train = np.array([0.95, 1.05, 1.25, 0.9, 1.15])
    
    model = RandomForestRegressor(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)
    return model

@app.route('/api/predict-premium', methods=['POST'])
def predict_premium():
    """
    Input: { elevation_risk, weather_risk, flood_history_risk }
    Output: { premium_multiplier }
    """
    global loaded_model
    
    try:
        data = request.json
        features = np.array([[
            data.get('elevation_risk', 1.0),
            data.get('weather_risk', 1.0),
            data.get('flood_history_risk', 1.0),
        ]])
        
        if loaded_model is None:
            loaded_model = train_model()
        
        prediction = loaded_model.predict(features)[0]
        
        return jsonify({
            'success': True,
            'premium_multiplier': float(max(0.8, min(1.5, prediction))),  # Clamp 0.8-1.5
            'message': 'Premium calculated successfully'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

### Step 4D: Run ML Service

```bash
cd backend/ml-service
source venv/bin/activate  # or: venv\Scripts\activate on Windows
python app.py

# Runs at: http://localhost:5000
```

---

## Part 5: Mock Disruption API Setup (Optional)

Create a mock API for simulating disruption events. Create `backend/mock-disruption-api/app.py`:

```python
from flask import Flask, jsonify
import random

app = Flask(__name__)

@app.route('/api/disruptionEvents', methods=['GET'])
def get_disruption_event():
    """Simulate disruption events for Parametric Automation"""
    
    zones = ['Velachery', 'Adyar', 'T-Nagar', 'Mylapore']
    
    return jsonify({
        'zone': random.choice(zones),
        'rain_mm': random.uniform(20, 120),  # 0-120 mm
        'status': random.choice(['GREEN', 'ORANGE_ALERT', 'RED_ALERT']),
        'strike': str(random.choice([True, False])).lower(),
        'timestamp': str(datetime.now())
    })

if __name__ == '__main__':
    from datetime import datetime
    app.run(host='0.0.0.0', port=8090, debug=True)
```

Run:
```bash
python backend/mock-disruption-api/app.py
```

---

## Part 6: Complete System Startup

### Quick Start (All Services)

```bash
# Terminal 1: MySQL (ensure it's running)
# Terminal 2: Backend
cd backend
mvn spring-boot:run

# Terminal 3: ML Service (optional)
cd backend/ml-service
source venv/bin/activate
python app.py

# Terminal 4: Mock Disruption API (optional)
cd backend/mock-disruption-api
python app.py

# Terminal 5: Frontend
cd frontend
npm start
```

### Services & URLs

| Service | URL | Port |
|---------|-----|----|
| Frontend | http://localhost:3000 | 3000 |
| Backend API | http://localhost:8080 | 8080 |
| ML Service | http://localhost:5000 | 5000 |
| Mock Disruption | http://localhost:8090 | 8090 |
| MySQL | localhost | 3306 |

---

## Part 7: Testing the Application

### User Registration Flow

1. **Access Frontend**: http://localhost:3000
2. **Click "Get covered in 3 minutes"**
3. **Enter Phone Number**: e.g., `9912345678`
4. **Verify OTP**: (OTP will be logged in backend console for testing)
5. **Step 1 - Identity**: 
   - Enter Aadhaar number: `999941057058` (Setu sandbox test number)
   - Send OTP
   - Enter OTP
   - Capture Selfie
6. **Step 2 - Work Verification**: 
   - Select Platform: RUSHDASH
   - Upload ID Card Image
7. **Step 3 - Zone Selection**:
   - Click on map to select working area
   - Choose zone from list
   - View calculated premium
8. **Step 4 - Complete**:
   - Enter UPI ID: `worker@okhdfcbank`
   - Nominee details
   - Accept policy terms

### Test Aadhaar Numbers (Setu Sandbox)

- **Male**: `999941057058`
- **Female**: `999971658847`

### Parametric Payout Test

Once a worker completes registration:

1. Mock Disruption API triggers
2. ParametricAutomationService detects disruption
3. Claims automatically created for affected zone
4. RazorpayPayoutService initiates UPI transfer
5. Worker receives bank notification with ₹500 (default claim amount)

---

## Part 8: Troubleshooting

### Issue: Backend fails to connect to MySQL

**Solution**:
```bash
# Check MySQL is running
mysql -u root -p -e "SELECT 1;"

# Verify credentials in application.yml
# Ensure payassure database exists
mysql -u root -p -e "USE payassure; SHOW TABLES;"
```

### Issue: Frontend cannot connect to Backend

**Solution**:
```bash
# Check backend is running on port 8080
lsof -i :8080

# Verify API_BASE_URL in frontend/src/services/api.js
```

### Issue: Setu Aadhaar OTP not working

**Solution**:
```bash
# Ensure SETU_CLIENT_ID and SETU_CLIENT_SECRET are set
# Check Setu dashboard: https://dashboard.setu.co

# For testing: Use sandbox credentials
echo $SETU_CLIENT_ID
echo $SETU_CLIENT_SECRET
```

### Issue: ML Service returns errors

**Solution**:
```bash
# Verify Python virtual environment is activated
source venv/bin/activate

# Check Flask is running on 5000
lsof -i :5000

# Test ML endpoint
curl http://localhost:5000/api/predict-premium -X POST \
  -H "Content-Type: application/json" \
  -d '{"elevation_risk": 1.0, "weather_risk": 1.0, "flood_history_risk": 1.0}'
```

---

## Features Breakdown

### Feature 1: Zone Selection
- Leaflet.js map of Chennai with predefined zones
- Click to select working area
- Real-time risk index display
- Base premium fetched from database

### Feature 2: ML-Driven Premium
- Elevation risk (Open-Elevation API)
- Weather forecast (OpenWeatherMap API)
- Historical flood data (Database)
- Random Forest model for final calculation

### Feature 3: Parametric Automation
- 60-second polling of Mock Disruption API
- Automatic claim trigger on disruption
- GPS validation of affected workers
- No manual OCR required

### Feature 4: Instant Payouts
- RazorpayX UPI transfer
- Automatic payment within 60 seconds of trigger
- Worker receives bank SMS
- Zero-touch payout process

### Feature 5: Enhanced Validation
- Aadhaar photo from UIDAI
- Selfie liveness check with confidence score
- Photo matching algorithm
- Only proceeds if match > 85%

---

## Environment Variables

Create a `.env` file in the backend root:

```bash
# MySQL
DB_USERNAME=root
DB_PASSWORD=extinction@7

# Setu Aadhaar API
SETU_CLIENT_ID=your-setu-client-id
SETU_CLIENT_SECRET=your-setu-client-secret

# Razorpay
RAZORPAY_KEY_ID=rzp_test_your_key
RAZORPAY_KEY_SECRET=your_secret

# OpenWeatherMap
OPENWEATHER_API_KEY=your_openweather_api_key

# Python ML Service
PYTHON_ML_SERVICE_URL=http://localhost:5000
```

---

## Production Deployment

### Backend (Docker)

```dockerfile
FROM maven:3.8-openjdk-17-slim as builder
WORKDIR /app
COPY backend .
RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend (Vercel/Netlify)

```bash
# Deploy to Vercel
npm install -g vercel
vercel

# Set REACT_APP_API_BASE_URL=https://api.payassure.com
```

### Python ML (Heroku/Railway)

```bash
# For production, use Gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app.py
```

---

## Support & Documentation

- **Setu Aadhaar API**: https://docs.setu.co/api/aadhaar
- **OpenWeatherMap**: https://openweathermap.org/api
- **Razorpay Payouts**: https://razorpay.com/docs/api/payouts/
- **Leaflet.js**: https://leafletjs.com/
- **Spring Boot**: https://spring.io/projects/spring-boot

---

## License

PayAssure © 2024. Parametric income insurance for gig workers.
