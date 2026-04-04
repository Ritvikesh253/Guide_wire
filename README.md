# PayAssure — Registration Module

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+ and npm

---

## 1. MySQL Setup

```sql
-- In MySQL shell:
CREATE DATABASE payassure CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or run the provided script:
```bash
mysql -u root -p < mysql-setup.sql
```

---

## 2. Backend (Spring Boot)

```bash
cd backend

# Set your MySQL credentials (or edit application.yml directly)
export DB_USERNAME=root
export DB_PASSWORD=yourpassword

# Set Setu API keys (Aadhaar KYC sandbox)
# Register free at: https://setu.co/products/kyc/aadhaar-api/
export SETU_CLIENT_ID=your-client-id
export SETU_CLIENT_SECRET=your-client-secret

# Run
./mvnw spring-boot:run
```

Backend starts on **http://localhost:8080**

Spring Boot will auto-create all MySQL tables on first run (`ddl-auto: update`).

---

## 3. Frontend (React)

```bash
cd frontend
npm install
npm start
```

Frontend starts on **http://localhost:3000**

API calls are proxied to `localhost:8080` (configured in `package.json`).

---

## 4. Test the Registration Flow

### Sandbox Aadhaar numbers (UIDAI test):
| Number | Gender |
|---|---|
| `999941057058` | Male — returns name: Ravi Kumar |
| `999971658847` | Female |

### Fake delivery ID format per platform:
| Platform | Partner ID format | Example |
|---|---|---|
| RushDash | `RSD-YYYY-XXXXXX` | `RSD-2024-084521` |
| QuickBite | `QBT-XXXXXXXXX` | `QBT-100293847` |
| ZipDeliver | `ZPD/CCC/XXXXXXX` | `ZPD/CHN/0039281` |

### UPI test: any string with `@` works (e.g. `testuser@upi`)

---

## 5. API Reference

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/otp/send` | Send phone OTP |
| POST | `/api/v1/auth/otp/verify` | Verify phone OTP |
| POST | `/api/v1/auth/aadhaar/otp` | Trigger UIDAI Aadhaar OTP |
| POST | `/api/v1/register/step1` | Aadhaar KYC + liveness |
| POST | `/api/v1/register/step2` | Work ID OCR verification |
| POST | `/api/v1/register/step3` | Zone + GPS setup |
| POST | `/api/v1/register/step4` | UPI + policy activation |
| POST | `/api/v1/auth/login` | Login → JWT token |
| GET  | `/api/v1/zones` | Available zones list |

---

## 6. Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |
| `SETU_CLIENT_ID` | Setu Aadhaar API client ID | — |
| `SETU_CLIENT_SECRET` | Setu Aadhaar API secret | — |
| `RAZORPAY_KEY_ID` | Razorpay key (Phase 2) | — |
| `RAZORPAY_KEY_SECRET` | Razorpay secret (Phase 2) | — |
