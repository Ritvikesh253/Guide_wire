-- PayAssure MySQL Setup
-- Run this before starting the Spring Boot application

CREATE DATABASE IF NOT EXISTS payassure
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE payassure;

-- Spring Boot with ddl-auto=update will create tables automatically.
-- This file is provided for reference / manual setup.

-- If you prefer manual creation, run this:

CREATE TABLE IF NOT EXISTS workers (
  id                    VARCHAR(36)  PRIMARY KEY,
  phone                 VARCHAR(15)  NOT NULL UNIQUE,
  password_hash         VARCHAR(255) NOT NULL,
  aadhaar_number        VARCHAR(12)  UNIQUE,
  legal_name            VARCHAR(100),
  aadhaar_address       TEXT,
  aadhaar_photo_url     TEXT,
  aadhaar_dob           VARCHAR(20),
  aadhaar_gender        VARCHAR(2),
  liveness_verified     BOOLEAN DEFAULT FALSE,
  platform              VARCHAR(20),
  partner_id            VARCHAR(50)  UNIQUE,
  platform_name_on_id   VARCHAR(50),
  work_verified         BOOLEAN DEFAULT FALSE,
  zone_id               VARCHAR(10),
  zone_name             VARCHAR(100),
  primary_lat           DOUBLE,
  primary_lon           DOUBLE,
  gps_permission_granted BOOLEAN DEFAULT FALSE,
  e_shram_uan           VARCHAR(30),
  upi_id                VARCHAR(100),
  penny_drop_verified   BOOLEAN DEFAULT FALSE,
  nominee_name          VARCHAR(100),
  nominee_phone         VARCHAR(15),
  policy_agreed         BOOLEAN DEFAULT FALSE,
  registration_status   VARCHAR(30) DEFAULT 'STEP_1_PENDING',
  active                BOOLEAN DEFAULT FALSE,
  created_at            DATETIME,
  activated_at          DATETIME
);

CREATE TABLE IF NOT EXISTS otp_records (
  id          VARCHAR(36)  PRIMARY KEY,
  phone       VARCHAR(15)  NOT NULL,
  otp         VARCHAR(6)   NOT NULL,
  expires_at  DATETIME     NOT NULL,
  used        BOOLEAN      DEFAULT FALSE
);

-- Index for fast OTP lookup
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_records(phone, used);
