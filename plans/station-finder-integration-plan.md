# Station Finder — Separate Backend Integration Plan

## Goal
Create a **dedicated Spring Boot backend + separate PostgreSQL database** for the station-finder Android app, completely independent from the main EV-Project backend. The station-finder will import data from OpenChargeMap and serve it via its own REST API — without touching the main app or its database.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
│         MAIN EV BACKEND             │     │      STATION FINDER BACKEND         │
│         (Spring Boot 3.3.5)         │     │      (New Spring Boot 3.3.5)        │
│                                     │     │                                     │
│  • JWT auth / OTP login             │     │  • No auth — fully public API      │
│  • Station mgmt (owner stations)    │     │  • OCM import service              │
│  • Bookings, payments, charging     │     │  • Station search / viewport      │
│  • WebSocket telemetry              │     │  • Station scoring                 │
│  • Owner dashboard, analytics       │     │  • Configurable sync schedule     │
│                                     │     │                                     │
│  DB: PostgreSQL (ev_project)        │     │  DB: PostgreSQL (ev_station_finder) │
│  Tables: stations, bookings,        │     │  Tables: stations, charger_slots   │
│          charger_slots, payments,   │     │         (simplified models only)    │
│          users, sessions, etc.      │     │                                     │
└────────────┬────────────────────────┘     └────────────┬────────────────────────┘
             │                                           │
             │                                           │
             ▼                                           ▼
┌──────────────────────┐                   ┌────────────────────────────┐
│  MAIN EV APP         │                   │  STATION FINDER APP        │
│  (Android, JWT auth) │                   │  (Android, no auth)         │
│                      │                   │                            │
│  • Full lifecycle    │                   │  • Map + station markers   │
│  • Book, charge, pay │                   │  • Station detail + nav    │
│  • Owner features    │                   │  • No auth needed          │
└──────────────────────┘                   └────────────────────────────┘
                                           ▲
                                           │
                                           ▼
                               ┌──────────────────────┐
                               │   OpenChargeMap API   │
                               │   api.ocm.io/v3/poi   │
                               │   (data source)       │
                               └──────────────────────┘
```

### Key Principles

| Principle | Why |
|-----------|-----|
| **Main backend is NEVER touched** | Zero risk to existing auth, bookings, payments |
| **Main database is NEVER touched** | Separate DB means zero schema conflicts |
| **Finder backend is self-contained** | Own models, own DB, own OCM key, own deployment |
| **No shared logic** | Simple, clean code with no `owner_id`, no JWT dependencies |

---

## 2. New Finder Backend — What It Contains

### 2.1 Technology Stack (Same as Main)

| Component | Choice |
|-----------|--------|
| Framework | Spring Boot 3.3.5 |
| Language | Java 21 |
| Database | PostgreSQL (separate database) |
| API | REST (no WebSocket, no STOMP needed) |
| Auth | **None** — fully public API |
| OCM Client | `RestTemplate` |
| Port | **8081** (main backend uses 8080) |

### 2.2 Data Model (Simplified)

Only **2 entities** — much simpler than the main backend:

**`Station`** — Core entity
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | Station name |
| latitude | Double | |
| longitude | Double | |
| address | String | Full address |
| operatingHours | String | e.g. "24 Hours", "6:00 AM - 10:00 PM" |
| pricePerKwh | Double | Default 0.0 |
| rating | Double | From OCM or default 0.0 |
| isOpen | Boolean | Calculated from operating hours |
| ocmId | Long | OCM's internal ID (for dedup) |
| ocmUuid | String | OCM's UUID (for dedup) |
| meta | String (JSON) | Extra OCM metadata (operator, usage type, etc.) |
| lastSynced | LocalDateTime | When this was last refreshed from OCM |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

**`ChargerSlot`** — Per-connector slots
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | |
| stationId | Long (FK) | References station |
| slotLabel | String | e.g. "CCS2 #1" |
| connectorType | String | e.g. "CCS2", "TYPE_2", "CHAdeMO" |
| powerKw | Double | |
| isAvailable | Boolean | Default true |

No `User`, `Owner`, `Booking`, `Payment`, `ChargingSession`, `Dispensary`, etc. — none of that complexity.

### 2.3 API Endpoints

| Method | Endpoint | Purpose | Returns |
|--------|----------|---------|---------|
| GET | `/api/stations/viewport?neLat&neLng&swLat&swLng` | Map markers in viewport | `List<StationMarker>` |
| GET | `/api/stations/nearby?lat&lng&radius&limit` | Nearest stations ranked by distance | `List<StationWithScore>` |
| GET | `/api/stations/{id}/detail?lat&lng` | Single station detail | `StationWithScore` |
| GET | `/api/stations/search?q&lat&lng&radius` | Text search by name/address | `List<StationWithScore>` |
| GET | `/api/stations/count` | Total station count | `{ count: number }` |
| POST | `/api/import/trigger?lat&lng&radius` | Manual OCM import trigger | `{ imported: number }` |

All endpoints return a consistent `ApiResponse` wrapper:
```json
{
  "success": true,
  "message": "Found 12 nearby stations",
  "data": [ ... ]
}
```

### 2.4 OCM Import Service

- Fetches from `https://api.openchargemap.io/v3/poi`
- Maps OCM data → `Station` + `ChargerSlot` entities
- Deduplicates by `ocmId` (skips if already imported)
- Runs on startup via `DataSeeder` (first run) and on schedule (daily)
- Can be triggered manually via POST endpoint

---

## 3. Station Finder App — Changes

The Android app currently calls OCM directly. It will be updated to call the **new finder backend** instead.

### 3.1 What Changes

| File | What Changes |
|------|-------------|
| `data/network/OpenChargeMapApi.kt` | Replace with `BackendApi.kt` (calls new backend instead of OCM) |
| `data/network/RetrofitClient.kt` | Replace with `BackendClient.kt` (points to `http://10.0.2.2:8081/`) |
| `data/model/OCMModels.kt` | Replace with `BackendModels.kt` (simpler models matching backend) |
| `data/repository/StationRepository.kt` | Change API calls from OCM to backend |
| `StationViewModel.kt` | Update type references from `OCMStation` → new models |
| `StationDetailsSheet.kt` | Accept new model type, display richer data |
| `MainActivity.kt` | Minor type updates |
| `build.gradle.kts` | Change `BACKEND_URL` endpoint |

### 3.2 What Does NOT Change

- **Main Android app** (`android/`) — 0 files changed ✅
- **Main backend** (`backend/`) — 0 files changed ✅
- **Station Finder UI structure** — Map + bottom sheet stays the same
- **Station Finder location logic** — Same LocationHelper, same GPS flow

### 3.3 Data Field Comparison

| Info | OCM (Current) | Finder Backend (New) | Station-Finder Shows |
|------|---------------|---------------------|---------------------|
| Name | ✅ `addressInfo.title` | ✅ `station.name` | ✅ Name |
| Operator | ✅ `operatorInfo.title` | ✅ `meta.ocm_operator` | ✅ Badge text |
| Address | ✅ `addressInfo.addressLine1` | ✅ `station.address` | ✅ Full address |
| Lat/Lng | ✅ `addressInfo.lat/lng` | ✅ `station.lat/lng` | ✅ Map markers |
| Connector types | ✅ `connections[].type.title` | ✅ `connectorTypes[]` (computed from slots) | ✅ Chip list |
| Power (kW) | ✅ `connections[].powerKw` | ✅ `slots[].powerKw` | ✅ Per-slot |
| Operating status | ✅ `status.isOperational` | ✅ `station.isOpen` | ✅ Open/Closed badge |
| Rating | ❌ Not available | ✅ `station.rating` | ✅ Stars (new!) |
| Distance | ❌ Client computes | ✅ `distance` | ✅ "X km away" |
| Available slots | ❌ Not available | ✅ `availableSlots` | ✅ "3/6 available" |
| Price/kWh | ❌ Rarely available | ✅ `station.pricePerKwh` | ✅ "₹16.5/kWh" |
| Operating hours | ❌ Rarely available | ✅ `station.operatingHours` | ✅ "24 Hours" |

---

## 4. Project Structure

```
ev-project/
├── backend/                          ← UNCHANGED (main EV backend)
│   └── src/main/java/com/ganesh/EV_Project/
│       ├── controller/
│       ├── service/
│       ├── model/
│       └── ...
│
├── station-finder-backend/           ← NEW (dedicated finder backend)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/com/ganesh/finder/
│   │   ├── FinderApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java      (no auth — permitAll)
│   │   │   ├── DataSeeder.java          (seed OCM stations)
│   │   │   └── StationSyncJob.java      (scheduled sync)
│   │   ├── controller/
│   │   │   ├── StationController.java   (finder API endpoints)
│   │   │   └── ImportController.java    (manual import trigger)
│   │   ├── model/
│   │   │   ├── Station.java             (simplified entity)
│   │   │   └── ChargerSlot.java         (simplified entity)
│   │   ├── dto/
│   │   │   ├── ApiResponse.java
│   │   │   ├── StationMarker.java
│   │   │   ├── StationWithScore.java
│   │   │   └── StationDetail.java
│   │   ├── repository/
│   │   │   ├── StationRepository.java
│   │   │   └── ChargerSlotRepository.java
│   │   └── service/
│   │       ├── StationImportService.java  (OCM fetch + transform)
│   │       └── StationService.java        (queries + scoring)
│   └── src/main/resources/
│       └── application.properties
│
├── station-finder/                   ← MODIFIED (Android app)
│   └── app/src/main/java/com/ganesh/stationfinder/
│       ├── data/network/
│       │   ├── BackendApi.kt         (NEW)
│       │   └── BackendClient.kt      (NEW)
│       ├── data/model/
│       │   └── BackendModels.kt      (NEW)
│       └── ...
│
├── android/                          ← UNCHANGED (main EV app)
└── web/                              ← UNCHANGED (web dashboard)
```

---

## 5. Implementation Phases

| Phase | What | New Files | Modified Files | Est. Time |
|-------|------|-----------|----------------|-----------|
| **1** | Create new Spring Boot project (structure, pom.xml, config) | 5 | 0 | 30 min |
| **2** | Data models + DTOs + Repositories | 8 | 0 | 45 min |
| **3** | StationImportService + DataSeeder | 2 | 1 | 1-2 hr |
| **4** | StationService (scoring, search) + StationController | 2 | 0 | 1 hr |
| **5** | StationSyncJob + application.properties | 1 | 1 | 30 min |
| **6** | Backend testing (curl all endpoints) | 0 | 0 | 1 hr |
| **7** | Station Finder Android app updates | 3 | 5 | 2-3 hr |
| **8** | Integration testing (both apps + both backends) | 0 | 0 | 1 hr |

**Total: ~8-10 hours**

---

## 6. Database Setup

### 6.1 Create Database

```sql
CREATE DATABASE ev_station_finder;
```

### 6.2 application.properties

```properties
spring.application.name=ev-station-finder

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/ev_station_finder
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server port (different from main backend's 8080)
server.port=8081

# OCM Configuration
ocm.api.key=${OCM_API_KEY}
ocm.sync.enabled=true
ocm.sync.radius-km=500
ocm.sync.country-id=101
ocm.sync.interval-cron=0 0 3 * * ?
```

---

## 7. Edge Cases & Gotchas

| Issue | Solution |
|-------|----------|
| **OCM API rate limits** | Backend caches in DB; sync is daily, not per-request |
| **Duplicate stations** | Dedup by `ocmId` — skip if already in DB |
| **OCM data is sparse** | Missing fields show "N/A" in the app |
| **Backend is offline** | Station-finder shows error toast + retry |
| **Two backends running** | Main on 8080, Finder on 8081 — no conflict |
| **GPS not available** | Fall back to default location (Mumbai: 19.0760, 72.8777) |
| **Docker deployment** | Two separate services in `docker-compose.yml` |
| **Production** | `ocm.api.key` must be set via env var, never hardcoded |

---

## 8. Summary of Changes

### Backend (New Project)
- **~14 new files** in `station-finder-backend/`
  - 1 `pom.xml`
  - 1 `Dockerfile`
  - 1 `FinderApplication.java`
  - 2 config files
  - 2 controllers
  - 2 entities (Station, ChargerSlot)
  - 4 DTOs
  - 2 repositories
  - 2 services
  - 1 `application.properties`

### Station Finder App
- **3 new files**: `BackendApi.kt`, `BackendClient.kt`, `BackendModels.kt`
- **5 modified files**: `StationRepository.kt`, `StationViewModel.kt`, `StationDetailsSheet.kt`, `MainActivity.kt`, `build.gradle.kts`
- **0 deleted files** (can keep `OCMModels.kt` for reference)

### Main Backend & Main Android App
- **0 files changed** ✅

---

## 9. Future Enhancements

| Feature | Benefit |
|---------|---------|
| Station photos (OCM or Google Places) | Visual confirmation of stations |
| User reviews (stored in finder DB) | Community ratings |
| Filter by connector type | "Show only CCS2" |
| Favorite stations | Pin frequently used stations |
| Offline cache | View last-seen stations without internet |
| Price filter | "Show stations under ₹20/kWh" |
