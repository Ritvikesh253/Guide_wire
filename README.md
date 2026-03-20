# Guide_wire
# GigShield
### Parametric Income Insurance for Gig Workers

> **We don't pay for broken bikes. We pay for broken livelihoods.**
> *When the city shuts down, your income doesn't have to.*

---

## The Problem

India has **11+ million gig delivery workers**. When a cyclone hits, a curfew is declared, or AQI crosses 400 — they can't work. They lose the day's earnings. No existing insurance product covers this gap.

Traditional insurance asks: *what did you lose, prove it, wait 3 weeks.*
GigShield asks one question: *did the disruption happen?*

If yes — **payout fires automatically.** The worker does nothing. The system does everything.

---

## How It Works — In One Line

> A measurable environmental or social disruption crosses a defined threshold → the system detects it → verifies the worker was active in the affected zone → calculates their personal income loss → pays them directly to their UPI wallet.

That's parametric insurance. No claim forms. No human review. No waiting.

---

## Coverage Scope

**What we cover — every event is verifiable from an external data source:**

- Heavy rainfall, flash floods, and cyclones
- Dangerous air pollution (AQI spikes above threshold)
- Government-declared curfews
- Local bandhs, strikes, and road blockades
- Market shutdowns and zone closures that eliminate delivery pickup points

**What we deliberately don't cover:**

- Health, medical, or life insurance
- Accidents, injury, or vehicle damage
- Any event requiring subjective human assessment

The boundary is intentional. Everything in scope can be confirmed by an API or a public data feed. Either the rain gauge crossed the threshold or it didn't. No worker ever needs to "prove" anything.

---

## System Architecture

```
┌────────────────────────────────────────────────────────────┐
│                  Gig Worker Mobile App                      │
│              (React Native — Android / iOS)                 │
└──────────────────────────┬─────────────────────────────────┘
                           │  REST API
                           ▼
┌────────────────────────────────────────────────────────────┐
│              Backend — Java Spring Boot                     │
│    Spring Web  │  Spring Security  │  Spring Scheduler      │
└────┬───────────┴──────────┬─────────┴──────────┬───────────┘
     │                      │                    │
     ▼                      ▼                    ▼
External Data APIs     PostgreSQL + PostGIS    Redis Cache
├── OpenWeatherMap      (geospatial zone       (zone lookups,
├── WAQI (AQI)           queries, ACID          API response
├── GDACS (disasters)    transactions)          caching)
├── IMD (India wx)
├── NewsAPI (curfews)
└── X API (social)
     │
     ▼
┌────────────────────────────────────────────────────────────┐
│                 Core Processing Pipeline                    │
│                                                            │
│   Disruption Detection  ──→  AI Risk Assessment            │
│           ↓                         ↓                      │
│   Worker Presence Check  ──→  Fraud Detection              │
│                    ↓                                       │
│         Parametric Trigger Engine (fully automated)        │
│                    ↓                                       │
│            Claim Auto-Generation                           │
│                    ↓                                       │
│         Razorpay / Cashfree UPI Payout                     │
└────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Reason |
|---|---|---|
| Backend | Java Spring Boot | Production-grade scheduling, REST, security |
| Database | PostgreSQL + PostGIS | Geospatial zone queries, ACID transactions |
| Cache | Redis | Sub-ms zone lookups, API response caching |
| Job Scheduler | Spring Scheduler / Quartz | API polling, weekly premium recalculation |
| ML Engine | Python scikit-learn | Isolation Forest fraud detection, pricing regression |
| Mobile App | React Native | Single codebase, Android-first for target demographic |
| Payments | Razorpay Payout API / Cashfree | Direct UPI disbursement, no intermediary |
| Geofencing | PostGIS + Google Maps Geocoding | Worker-in-zone detection at pin-code granularity |

---

## Core Systems — Deep Breakdown

---

### 1. Disruption Detection Engine

Runs as a Spring Scheduler job, polling external APIs at fixed intervals. A disruption is only confirmed when the measured parameter crosses the threshold for a **sustained window** — a 5-minute rain spike does not trigger a payout.

**Trigger thresholds by event type:**

| Disruption | Data source | Threshold |
|---|---|---|
| Rain (partial payout) | OpenWeatherMap | > 15 mm/hr sustained 20 min |
| Rain (full payout) | OpenWeatherMap | > 40 mm/hr |
| Flood | GDACS RSS feed | Alert level 1+ |
| Cyclone | IMD / GDACS | Category 1+ within 100 km of zone |
| Air pollution | WAQI API | AQI > 300 |
| Curfew | NewsAPI + district collector feeds | Confirmed keyword match in zone |
| Strike / bandh | NewsAPI + X API geo-search | Delivery volume drop > 60% in zone |

**On confirmation, the engine:**
- Defines the affected zone as a PostGIS polygon (city, district, or pin-code level)
- Timestamps the disruption window start and projected end
- Publishes an event to the internal processing pipeline
- Logs raw source data for full auditability and future model training

---

### 2. Worker Presence Verification

GPS alone is not enough — coordinates can be spoofed. Three signals are layered and combined into a single **presence confidence score.**

**Signal 1 — GPS trail continuity**
- App pings GPS every 90 seconds while active
- Real delivery movement shows variance: acceleration, deceleration, stops at restaurants and drop points, route shapes matching known delivery corridors
- Spoofed GPS is typically static or moves with unnatural uniformity
- Workers with pre-event movement variance below the minimum threshold are excluded at this gate

**Signal 2 — Delivery activity correlation**
- Pickup timestamps, drop-off locations, and order completions are cross-referenced against the disruption zone and time window
- A worker actively completing orders in the affected zone in the 45 minutes before the trigger fires is considered presence-confirmed
- This is the hardest signal to fake — it requires real interaction with the delivery platform's order management system

**Signal 3 — AI presence confidence score**

```
presence_score =
    (0.4 × gps_continuity_score)
  + (0.4 × delivery_activity_score)
  + (0.2 × historical_zone_match_score)
```

| Score | Decision |
|---|---|
| ≥ 0.75 | Auto-eligible → immediate pipeline |
| 0.50 – 0.74 | Eligible → 4-hour automated hold |
| < 0.50 | Held → manual review queue |

---

### 3. AI Risk Assessment and Weekly Pricing

**Payout calculation — personal, not flat rate:**

Every worker's payout is derived from their own earnings history. A worker averaging ₹800/day receives a different payout than one averaging ₹300/day for the same disruption event in the same zone.

```
payout =
    worker_30d_avg_hourly_earning
  × disruption_severity_multiplier
  × hours_disrupted
  × presence_score_weight
```

- The severity multiplier is trained on historical data — 20 mm/hr rain in a flood-prone low-drainage pincode has a higher multiplier than the same intensity in a well-drained zone
- Payout is always capped at 110% of the worker's own 30-day earning average (this ceiling also serves as the fraud signal described in Section 4)

**Weekly premium recalculation (every Monday, Spring Scheduler):**

Each worker's premium for the coming week is computed from:
- 7-day weather forecast for their primary operating zone (OpenWeatherMap Forecast API)
- Worker's personal claim history and current fraud score
- Zone-level risk index (updated quarterly from historical event data)
- Seasonal adjustment factor (monsoon months carry a higher baseline multiplier)

Higher forecasted disruption probability → higher weekly premium. Low-risk week → lower premium. Workers pay for their actual exposure, not a static annual average.

---

### 4. Fraud Detection System

#### Individual-level signals

| Signal | What it catches | Response |
|---|---|---|
| Static GPS before event | Location spoofing | Hard reject |
| App installed < 72 hours before claim | New account farming | Ineligible for coverage |
| Claim amount > 110% of 30-day personal avg | Inflated earnings history | Capped + flagged |
| GPS present, zero delivery activity | Presence-only spoofing | Hold + review |
| GPS path identical to another account | Copy-paste coordinate fraud | Both accounts held |
| App activated only at disruption start | Opportunistic spoofing | Flagged as suspicious |

#### Ring-level signals — coordinated attacks

A single spoofed account is noise. Five hundred coordinated fake accounts during a real cyclone is an existential threat to the liquidity pool. Coordinated rings leave coordinated signals — and those signals compound across detection gates.

**Surge detection:**
- Workers claiming coverage in a zone exceeding **2.5× the historical average** for that zone, time-of-day, and day-of-week triggers a ring investigation flag
- Real disruptions cause workers to *stop working* — they do not cause a sudden multiplication of active coverage claimants

**Device and network clustering:**
- Accounts sharing the same hardware device ID, IP subnet, or app installation timestamp cluster (within 10-minute windows) are cross-referenced at claim time
- If more than 5 accounts share any two of these identifiers → all linked accounts are simultaneously held pending investigation

**Order volume triangulation — the strongest ring signal:**
- If rain is severe enough to ground 500 workers, completed delivery orders in that zone must also drop proportionally
- The system computes the ratio of worker-claims to actual order-volume drop in real time
- A genuine disruption shows proportional decline across both metrics simultaneously
- A fraud ring inflates worker claims while the order data remains inconsistent

```
ring_suspicion_score =
    (claimed_workers_in_zone / historical_avg_workers_in_zone)
  ÷ (1 - order_volume_drop_ratio)

score > 3.0 → ring investigation triggered, zone payouts paused
```

#### Protecting honest workers caught near a fraud ring

- **Flagged ≠ rejected** — workers caught in the same zone as a fraud ring but who pass all individual behavioral checks receive payout on a **48-hour hold**
- If cleared after review: full payout is released + a **5% goodwill supplement** for the delay
- No claim is ever rejected solely because of zone proximity to fraud activity
- The hold policy and goodwill process are disclosed in policy terms at signup

---

### 5. Progressive Payout System

Trust is built over time. New accounts receive more scrutiny. Established accounts with clean history get faster payouts.

| Worker tenure | Fraud score | Payout schedule |
|---|---|---|
| < 30 days | Any | 24-hour hold → manual review |
| 30 – 90 days | Clean | 4-hour hold → automated checks |
| 90+ days | Clean | Immediate disbursement |
| Any tenure | Flagged | Manual review, held until cleared |

**Additional controls:**
- All payouts carry a **7-day clawback window** — if post-event analysis confirms fraud, the payout is reversed
- Clawback policy is disclosed at onboarding and included in policy terms
- Payment is direct UPI transfer via Razorpay Payout API — no intermediary wallet, no withdrawal delay

---

## Adversarial Defense and Anti-Spoofing Strategy

> *500 delivery workers. Fake GPS. A real cyclone. Every individual claim looks legitimate — the disruption is confirmed, the accounts exist, the coordinates are in the zone. Without layered defense, the liquidity pool drains in a single event.*

Our defense is a **five-gate funnel.** Each gate targets a different attack vector. The cost of passing all five simultaneously exceeds any realistic payout value.

```
Disruption confirmed by external APIs
                │
                ▼
 ┌── GATE 1: Pre-event activity ──────────────────────────┐
 │  Was the worker actively delivering in the last 45 min? │
 │  Is GPS movement realistic (variance, route shape)?     │
 │  FAIL → Hard reject                                     │
 └──────────────────────────┬─────────────────────────────┘
                            │ PASS
                            ▼
 ┌── GATE 2: Policy age ──────────────────────────────────┐
 │  Is the policy older than 72 hours?                     │
 │  FAIL → Ineligible (new account farm)                   │
 └──────────────────────────┬─────────────────────────────┘
                            │ PASS
                            ▼
 ┌── GATE 3: Ring detection ──────────────────────────────┐
 │  Worker surge > 2.5× zone historical average?           │
 │  Device / IP clustering across accounts?                │
 │  FAIL → All linked accounts held, investigation opens   │
 └──────────────────────────┬─────────────────────────────┘
                            │ PASS
                            ▼
 ┌── GATE 4: Earning history ─────────────────────────────┐
 │  Is claim within 110% of personal 30-day average?       │
 │  FAIL → Claim capped, account flagged                   │
 └──────────────────────────┬─────────────────────────────┘
                            │ PASS
                            ▼
 ┌── GATE 5: Order volume ratio ──────────────────────────┐
 │  Are claimant numbers proportional to actual            │
 │  order drop in the zone?                                │
 │  FAIL → ring_suspicion_score > 3.0, zone paused        │
 └──────────────────────────┬─────────────────────────────┘
                            │ PASS
                            ▼
       Progressive payout (instant / 4-hour / 24-hour hold)
```

**The core insight that makes this work:**

Simple GPS verification fails against coordinated fraud because the disruption is real — the fraud is in the fake *presence*, not the fake weather. Our architecture separates two distinct problems:

- Disruption confirmation → solved by external APIs (objective, tamper-proof)
- Worker presence verification → solved by behavioral data (extremely hard to fake at scale)

A spoofed GPS coordinate places a worker in a zone. It cannot fabricate 45 minutes of realistic delivery movement, restaurant stop patterns, and order completion timestamps — all cross-referenced against a live order management system.

---

## Database Schema

```sql
-- Core entities
workers           (id, name, phone, zone_id, avg_daily_earning, wallet_id,
                   policy_age_days, fraud_score, created_at)

policies          (id, worker_id, start_date, end_date, weekly_premium,
                   status, claim_count)

zones             (id, name, boundary GEOMETRY(POLYGON, 4326), city,
                   state, risk_index, last_updated)

-- Event and activity tracking
disruption_events (id, type, zone_id, severity, started_at, ended_at,
                   data_source, raw_payload, confirmed)

gps_logs          (id, worker_id, lat, lon, timestamp,
                   movement_variance_score)

delivery_logs     (id, worker_id, pickup_zone, drop_zone,
                   completed_at, order_value)

-- Claims and payouts
claims            (id, worker_id, event_id, payout_amount, presence_score,
                   fraud_score, status, hold_until)

payout_log        (id, claim_id, upi_ref, amount, disbursed_at,
                   clawback_deadline, reversed)
```

---

## External APIs

| API | Purpose | Base URL |
|---|---|---|
| OpenWeatherMap | Rain intensity, 7-day forecast | `api.openweathermap.org` |
| WAQI | Real-time AQI — Indian cities | `api.waqi.info` |
| GDACS | Flood, cyclone, disaster alerts (RSS) | `gdacs.org/xml/rss.xml` |
| IMD | Official India meteorological data | `mausam.imd.gov.in` |
| NewsAPI | Curfew, bandh, strike keyword monitoring | `newsapi.org` |
| X API v2 | Geo-tagged social disruption signals | `api.twitter.com/2` |
| Razorpay Payout | Direct UPI wallet disbursement | `api.razorpay.com` |
| Google Maps Geocoding | Zone boundary and address resolution | `maps.googleapis.com` |

---

## API Endpoints (Spring Boot REST)

```
Authentication
POST  /api/v1/auth/register              Register worker account
POST  /api/v1/auth/login                 JWT token issue

Workers and Policy
GET   /api/v1/workers/{id}/policy        Active policy and coverage status
POST  /api/v1/workers/{id}/location      Push GPS coordinate from mobile app
GET   /api/v1/premium/quote/{workerId}   Next week's premium quote
POST  /api/v1/premium/pay               Record premium payment

Disruptions
GET   /api/v1/disruptions/active         Currently active disruption zones
GET   /api/v1/disruptions/{id}           Disruption detail + zone boundary

Claims and Payouts
GET   /api/v1/claims/{workerId}          Worker's full claim history
GET   /api/v1/claims/{claimId}/status    Live claim status + payout ETA

Admin
GET   /api/v1/admin/fraud/flagged        All accounts in hold/review queue
POST  /api/v1/admin/fraud/resolve        Clear or reject a held claim
GET   /api/v1/admin/zones/{id}/stats     Zone-level event and claim stats
```

---

## Project Structure (Spring Boot)

```
gigshield-backend/
├── src/main/java/com/gigshield/
│   ├── controller/
│   │   ├── WorkerController.java
│   │   ├── ClaimController.java
│   │   ├── DisruptionController.java
│   │   └── AdminController.java
│   │
│   ├── service/
│   │   ├── DisruptionDetectionService.java    ← polls all external APIs
│   │   ├── PresenceVerificationService.java   ← GPS + delivery scoring
│   │   ├── FraudDetectionService.java         ← individual + ring signals
│   │   ├── PayoutService.java                 ← trigger + Razorpay call
│   │   └── PremiumPricingService.java         ← weekly recalculation
│   │
│   ├── model/
│   │   ├── Worker.java
│   │   ├── Policy.java
│   │   ├── Claim.java
│   │   ├── DisruptionEvent.java
│   │   └── Zone.java
│   │
│   ├── repository/                            ← Spring Data JPA
│   │
│   ├── scheduler/
│   │   ├── DisruptionPollingJob.java          ← @Scheduled — every 15 min
│   │   └── PremiumRecalculationJob.java       ← @Scheduled — every Monday
│   │
│   └── config/
│       ├── SecurityConfig.java
│       └── RedisConfig.java
│
└── src/main/resources/
    └── application.yml
```

---

## Getting Started

**Prerequisites:** Java 17+, PostgreSQL 14+ with PostGIS extension, Redis 7+

```bash
# Clone the repository
git clone https://github.com/your-org/gigshield-backend.git
cd gigshield-backend

# Copy and configure environment
cp src/main/resources/application.yml.example src/main/resources/application.yml
# → Add API keys: OpenWeatherMap, WAQI, Razorpay, DB credentials

# Run locally
./mvnw spring-boot:run

# Or with Docker Compose (includes Postgres + Redis)
docker-compose up
```

**Required environment variables:**

```bash
OPENWEATHER_API_KEY=
WAQI_API_KEY=
NEWSAPI_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
DB_URL=jdbc:postgresql://localhost:5432/gigshield
DB_USERNAME=
DB_PASSWORD=
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## What Makes This Different

Most insurance tech is process automation — you still file a claim, a queue still reviews it, you still wait. GigShield is a fundamentally different architecture.

**The worker never initiates anything**
- No claim form, no upload, no support ticket
- The event triggers the system — not a worker action

**Payout is personal, not flat**
- Each payout is based on the individual worker's own 30-day earning history
- A high-earning worker and a part-time worker receive proportionally different payouts for the same disruption event in the same zone

**Pricing is forward-looking**
- Premiums reflect next week's forecast risk, not a static annual average
- Workers pay for actual exposure — low-risk weeks are cheaper

**Fraud defense scales with attack size**
- Individual fraud is caught by behavioral signals
- Coordinated ring fraud is caught by aggregate anomaly detection
- The larger and more coordinated the ring, the louder and more distinct the signal

**Honest workers are protected by design**
- Every fraud gate has an explicit hold-not-reject path for ambiguous cases
- Cleared workers receive a goodwill supplement for the delay
- Zone proximity to fraud is never sufficient grounds for rejection

---

## Team

Built for **[Hackathon Name]** · [Date]

| Name | Role |
|---|---|
| [Name] | Backend — Java Spring Boot |
| [Name] | Mobile App — React Native |
| [Name] | ML / Fraud Engine — Python |
| [Name] | System Design / Product |

---

*GigShield — because the gig economy runs on trust, and trust runs on money showing up when it's supposed to.*
