-- PayAssure MySQL Schema with Spatial Extensions
-- Includes all 5 features: Zone Selection, ML Premiums, Parametric Automation, Instant Payouts, Aadhaar Validation

CREATE DATABASE IF NOT EXISTS payassure
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE payassure;

-- ── WORKERS  ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS workers (
  id                    VARCHAR(36)  PRIMARY KEY,
  phone                 VARCHAR(15)  NOT NULL UNIQUE,
  password_hash         VARCHAR(255) NOT NULL,
  
  -- Aadhaar KYC (Feature: Aadhaar ID Matching)
  aadhaar_number        VARCHAR(12)  UNIQUE,
  aadhaar_photo_base64  LONGTEXT,     -- Stored from UIDAI
  selfie_photo_base64   LONGTEXT,     -- Captured during registration
  selfie_verified       BOOLEAN DEFAULT FALSE,
  selfie_liveness_confidence DECIMAL(5, 4),  -- 0.0 to 1.0
  aadhaar_match_confidence   DECIMAL(5, 4),  -- Aadhaar photo vs Selfie match
  
  legal_name            VARCHAR(100),
  aadhaar_address       TEXT,
  aadhaar_dob           VARCHAR(20),
  aadhaar_gender        VARCHAR(2),
  
  -- Platform Verification
  platform              VARCHAR(20),   -- RUSHDASH / QUICKBITE / ZIPDELIVER
  partner_id            VARCHAR(50)  UNIQUE,
  platform_name_on_id   VARCHAR(50),
  work_verified         BOOLEAN DEFAULT FALSE,
  
  -- Zone Selection (Feature 1)
  zone_id               VARCHAR(36),
  zone_name             VARCHAR(100),
  primary_lat           DOUBLE,
  primary_lon           DOUBLE,
  gps_permission_granted BOOLEAN DEFAULT FALSE,
  
  -- Pyment Info
  e_shram_uan           VARCHAR(30),
  upi_id                VARCHAR(100),
  penny_drop_verified   BOOLEAN DEFAULT FALSE,
  
  -- Nominee
  nominee_name          VARCHAR(100),
  nominee_phone         VARCHAR(15),
  
  -- Policy
  policy_agreed         BOOLEAN DEFAULT FALSE,
  registration_status   VARCHAR(30) DEFAULT 'STEP_1_PENDING',  -- STEP_1, STEP_2, STEP_3, STEP_4, ACTIVE, REJECTED
  active                BOOLEAN DEFAULT FALSE,
  
  created_at           DATETIME,
  activated_at         DATETIME,
  
  FOREIGN KEY (zone_id) REFERENCES zones(id),
  INDEX idx_phone (phone),
  INDEX idx_aadhaar (aadhaar_number),
  INDEX idx_zone (zone_id),
  INDEX idx_active (active)
);

-- ── OTP RECORDS ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS otp_records (
  id          VARCHAR(36)  PRIMARY KEY,
  phone       VARCHAR(15)  NOT NULL,
  otp         VARCHAR(6)   NOT NULL,
  type        VARCHAR(20),  -- 'SMS' / 'UIDAI'
  expires_at  DATETIME     NOT NULL,
  used        BOOLEAN DEFAULT FALSE,
  created_at  DATETIME,
  matched_at  DATETIME,
  
  INDEX idx_phone_used (phone, used),
  INDEX idx_expires_at (expires_at)
);

-- ── ZONES (Feature 1: Zone Selection) ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS zones (
  id                    VARCHAR(36)  PRIMARY KEY,
  name                  VARCHAR(100) NOT NULL UNIQUE,
  city                  VARCHAR(50)  NOT NULL,
  
  -- Risk Metrics
  risk_index            DECIMAL(3, 2) NOT NULL,  -- 0.5 to 2.0
  base_weekly_premium   DECIMAL(10, 2) NOT NULL,
  
  -- Geospatial (GeoJSON Polygon for ST_Contains checks)
  geo_json_polygon      LONGTEXT,
  center_lat            DOUBLE,
  center_lon            DOUBLE,
  
  active                BOOLEAN DEFAULT TRUE,
  created_at            DATETIME,
  updated_at            DATETIME,
  
  INDEX idx_city (city),
  INDEX idx_active (active)
);

-- ── FLOOD HISTORY (Feature 2: ML Premium Calculation) ──────────────────────
CREATE TABLE IF NOT EXISTS flood_history (
  id                    VARCHAR(36)  PRIMARY KEY,
  zone_id               VARCHAR(36)  NOT NULL,
  flood_date            DATE         NOT NULL,
  rain_mm               DECIMAL(10, 2),
  severity              VARCHAR(20),  -- LOW / MODERATE / HIGH / CRITICAL
  affected_worker_count INT,
  claims_processed      INT,
  total_payout          DECIMAL(15, 2),
  
  created_at            DATETIME,
  
  FOREIGN KEY (zone_id) REFERENCES zones(id),
  INDEX idx_zone (zone_id),
  INDEX idx_flood_date (flood_date)
);

-- ── DISRUPTION EVENTS (Feature 3: Parametric Automation) ────────────────────
CREATE TABLE IF NOT EXISTS disruption_events (
  id                    VARCHAR(36)  PRIMARY KEY,
  zone_id               VARCHAR(36)  NOT NULL,
  event_type            VARCHAR(50),  -- RAIN / FLOOD / STRIKE / CURFEW
  
  detection_time        DATETIME     NOT NULL,
  rain_mm               DECIMAL(10, 2),
  status                VARCHAR(30),  -- RED_ALERT / ORANGE_ALERT / GREEN
  strike_active         BOOLEAN DEFAULT FALSE,
  magnitude             DECIMAL(5, 2),
  
  claims_triggered      INT DEFAULT 0,
  payouts_processed     INT DEFAULT 0,
  
  created_at            DATETIME,
  
  FOREIGN KEY (zone_id) REFERENCES zones(id),
  INDEX idx_zone (zone_id),
  INDEX idx_detection_time (detection_time),
  INDEX idx_status (status)
);

-- ── ACTIVE POLICIES ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS active_policies (
  id                    VARCHAR(36)  PRIMARY KEY,
  worker_id             VARCHAR(36)  NOT NULL,
  zone_id               VARCHAR(36)  NOT NULL,
  
  -- Premium (Feature 2: ML Calculation)
  base_premium          DECIMAL(10, 2) NOT NULL,
  elevation_risk_mult   DECIMAL(3, 2),   -- From Open-Elevation API
  weather_risk_mult     DECIMAL(3, 2),   -- From OpenWeatherMap
  flood_history_mult    DECIMAL(3, 2),   -- From flood_history table
  ml_calculated_premium DECIMAL(10, 2),  -- Final premium from ML model
  
  coverage_amount       DECIMAL(15, 2),  -- ₹50,000 base
  deductible            DECIMAL(10, 2),
  
  policy_start          DATE NOT NULL,
  policy_end            DATE,
  status                VARCHAR(20),  -- ACTIVE / SUSPENDED / EXPIRED / CLAIMED
  
  created_at            DATETIME,
  last_premium_charged  DATETIME,
  
  FOREIGN KEY (worker_id) REFERENCES workers(id),
  FOREIGN KEY (zone_id) REFERENCES zones(id),
  INDEX idx_worker (worker_id),
  INDEX idx_zone (zone_id),
  INDEX idx_status (status)
);

-- ── CLAIMS (Feature 3 & 4: Parametric + Payouts) ──────────────────────────
CREATE TABLE IF NOT EXISTS claims (
  id                    VARCHAR(36)  PRIMARY KEY,
  worker_id             VARCHAR(36)  NOT NULL,
  policy_id             VARCHAR(36)  NOT NULL,
  disruption_event_id   VARCHAR(36),
  
  -- Claim Trigger
  claim_type            VARCHAR(50),  -- PARAMETRIC / MANUAL
  trigger_reason        VARCHAR(100),  -- RAIN>70MM / STRIKE / CURFEW
  
  -- Validation
  worker_gps_lat        DOUBLE,
  worker_gps_lon        DOUBLE,
  gps_zone_match        BOOLEAN,      -- Was worker in affected zone?
  
  -- Payment (Feature 4: Instant Payouts)
  claim_amount          DECIMAL(15, 2) NOT NULL,
  payout_id             VARCHAR(100),
  payout_status         VARCHAR(30),  -- INITIATED / PROCESSING / COMPLETED / FAILED
  payout_timestamp      DATETIME,
  
  status                VARCHAR(30),  -- PENDING / APPROVED / REJECTED / PAID
  
  created_at            DATETIME,
  approved_at           DATETIME,
  paid_at               DATETIME,
  
  FOREIGN KEY (worker_id) REFERENCES workers(id),
  FOREIGN KEY (policy_id) REFERENCES active_policies(id),
  FOREIGN KEY (disruption_event_id) REFERENCES disruption_events(id),
  INDEX idx_worker (worker_id),
  INDEX idx_payout_status (payout_status),
  INDEX idx_status (status)
);

-- ── PAYOUT RECEIPTS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payout_receipts (
  id                    VARCHAR(36)  PRIMARY KEY,
  claim_id              VARCHAR(36)  NOT NULL,
  worker_id             VARCHAR(36)  NOT NULL,
  
  razorpay_payout_id    VARCHAR(100),
  razorpay_contact_id   VARCHAR(100),
  razorpay_fund_account_id VARCHAR(100),
  
  upi_id                VARCHAR(100),
  amount_rupees         DECIMAL(10, 2),
  
  status                VARCHAR(30),  -- INITIATED / PROCESSING / PROCESSED / FAILED
  failure_reason        TEXT,
  
  initiated_at          DATETIME,
  completed_at          DATETIME,
  bank_reference        VARCHAR(100),
  
  FOREIGN KEY (claim_id) REFERENCES claims(id),
  FOREIGN KEY (worker_id) REFERENCES workers(id),
  INDEX idx_worker (worker_id),
  INDEX idx_status (status)
);

-- ── Insert Sample Zones  ─────────────────────────────────────────────────────
INSERT IGNORE INTO zones (id, name, city, risk_index, base_weekly_premium, center_lat, center_lon, active) VALUES
('z-01', 'Adyar', 'Chennai', 1.5, 68, 13.0019, 80.2431, TRUE),
('z-02', 'T-Nagar', 'Chennai', 0.9, 42, 13.1018, 80.2290, TRUE),
('z-03', 'Guindy', 'Chennai', 0.7, 29, 13.0010, 80.2080, TRUE),
('z-04', 'Velachery', 'Chennai', 1.4, 63, 12.9708, 80.2272, TRUE),
('z-05', 'Mylapore', 'Chennai', 1.0, 46, 13.0345, 80.2700, TRUE),
('z-06', 'Besant Nagar', 'Chennai', 0.85, 38, 13.0105, 80.2630, TRUE),
('z-07', 'Perambur', 'Chennai', 1.2, 55, 13.1370, 80.3014, TRUE),
('z-08', 'Madipakkam', 'Chennai', 1.3, 58, 12.9490, 80.1790, TRUE);

-- ── Verify Spatial Extensions ────────────────────────────────────────────────
-- To enable spatial queries with ST_Contains, run in MySQL:
-- ALTER TABLE zones ADD COLUMN zone_polygon GEOMETRY NOT NULL SRID 4326;
-- ALTER TABLE zones ADD SPATIAL INDEX idx_zone_polygon (zone_polygon);
-- Then use: SELECT * FROM zones WHERE ST_Contains(zone_polygon, ST_Point(lat, lon));

COMMIT;
