# Station Finder — Backend Integration Plan

## Goal
Make the standalone **station-finder** Android app use the **shared backend** (instead of calling OpenChargeMap directly), without touching the main EV Android app's code or disrupting its backend logic.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SHARED BACKEND                                   │
│                         (Spring Boot 3.3.5)                             │
│                                                                         │
│  ┌─────────────────┐    ┌───────────────────┐    ┌──────────────────┐  │
│  │  Finder APIs     │    │  Authenticated APIs│    │  OCM Sync Svc   │  │
│  │  (no auth)       │    │  (JWT required)    │    │  (scheduled)     │  │
│  │                  │    │                    │    │                  │  │
│  │  GET /finder/    │    │  GET /api/stations │    │  ─→ fetch OCM    │  │
│  │    stations/*    │    │  POST /api/bookings│    │  ─→ save to DB   │  │
│  │                  │    │  POST /api/charging│    │  ─→ update meta   │  │
│  └────────┬─────────┘    └────────┬───────────┘    └──────────────────┘  │
│           │                       │                                      │
│           └───────────┬───────────┘                                      │
│                       │                                                  │
│              ┌────────▼────────┐                                         │
│              │  PostgreSQL DB  │                                         │
│              │  (stations +    │                                         │
│              │   slots + books)│                                         │
│              └─────────────────┘                                         │
└──────────────────────────────────────────────────────────────────────────┘
           │                                      ▲
           │                                      │
           ▼                                      │
┌──────────────────────┐          ┌────────────────────────────┐
│  Station Finder App   │          │  Main EV Android App       │
│  (separate project)   │          │  (unchanged — untouched)   │
│                       │          │                            │
│  • No auth required   │          │  • JWT auth                │
│  • Finder APIs only   │          │  • Full lifecycle          │
│  • Map + details      │          │  • Bookings, charging,     │
│  • Bottom sheet UI    │          │    payments, profile, etc. │
└──────────────────────┘          └────────────────────────────┘
```

### Key Principles

| Principle | Why |
|-----------|-----|
| **Main app is NEVER touched** | Zero risk of breaking existing features |
| **Backend gets new finder endpoints** | Station-finder calls these without auth |
| **Backend owns OCM data** | API key stays server-side, not in the app |
| **Same DB, same stations** | Both apps share real station data from one source |

---

## 2. Backend — New Code (Additive, No Breaking Changes)

### 2.1 New Finder Controller — `StationFinderController.java`

A new controller mapped to `/api/finder/stations` with **no authentication required**.

| Endpoint | Purpose | Returns |
|----------|---------|---------|
| `GET /api/finder/stations/viewport?neLat&neLng&swLat&swLng` | Map markers in viewport | `List<StationMarkerDTO>` (id, name, lat, lng, available) |
| `GET /api/finder/stations/nearby?lat&lng&radius&limit` | Nearest stations with scores | `List<StationScoreDTO>` (full data with distance, slots, rating) |
| `GET /api/finder/stations/{id}/detail?lat&lng` | Single station detail | `StationScoreDTO` |
| `GET /api/finder/stations/search?q&lat&lng&radius` | Text search by name/address | `List<StationScoreDTO>` |
| `GET /api/finder/stations/count` | Total station count | `{ count: number }` |

These endpoints **internally call the same existing services** (`StationRecommendationService`, `StationService`). No logic duplication.

### 2.2 Security Config Update

In `SecurityConfig.java`, add:
```java
.requestMatchers("/api/finder/**").permitAll()
```

This is the **only change** to existing auth code — purely additive, doesn't affect any existing `/api/...` routes.

### 2.3 New Service — `StationImportService.java`

A service that fetches stations from OpenChargeMap and stores them in the backend's PostgreSQL database.

**Responsibilities:**
- Fetch from OCM API (`https://api.openchargemap.io/v3/poi`)
- Transform OCM data → backend `Station` model
- Deduplicate by OCM UUID (skip if already imported)
- Create a default "dummy" `ChargerSlot` per connector (so slot availability logic works)
- Store import metadata (source = "OCM", last synced timestamp)

**OCM → Station Model Mapping:**

| OCM Field | Backend Station Field | Notes |
|-----------|----------------------|-------|
| `AddressInfo.Title` | `name` | Station name |
| `AddressInfo.Latitude` | `latitude` | |
| `AddressInfo.Longitude` | `longitude` | |
| `AddressInfo.AddressLine1 + Town` | `address` | Concatenated |
| `OperatorInfo.Title` | `meta` (JSON) | Stored as `{"ocm_operator": "..."}` |
| `UsageType.Title` | `meta` (JSON) | `{"ocm_usage_type": "..."}` |
| `Connections[].PowerKW` | `pricePerKwh` | Set to 0 (pricing from OCM is unreliable) |
| `GeneralComments` | `meta` (JSON) | `{"ocm_comments": "..."}` |
| `Connections[].ConnectionType` | `ChargerSlot.connectorType` | Creates one slot per connection type |
| `Connections[].StatusType.IsOperational` | — | Only imports operational stations |
| (OCM ID) | `meta` (JSON) | `{"ocm_id": 123456}` — used for dedup |

### 2.4 New Scheduled Task — `StationSyncJob.java`

A scheduled job that periodically syncs new/updated stations from OpenChargeMap.

```java
@Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
public void syncStations() {
    // 1. Fetch stations updated in last 24h from OCM
    // 2. Upsert into local DB
    // 3. Log stats
}
```

Configurable via `application.properties`:
```properties
ocm.api.key=${OCM_API_KEY}
ocm.sync.enabled=true
ocm.sync.radius-km=500  # Default search radius
ocm.sync.country-id=101  # India
```

### 2.5 DataSeeder Enhancement (Optional)

Add a one-time import of popular Indian EV stations on first startup:
```java
if (stationRepository.count() == 0) {
    stationImportService.importFromOCM(19.0760, 72.8777, 50); // Mumbai area
    stationImportService.importFromOCM(12.9716, 77.5946, 50); // Bangalore area
}
```

---

## 3. Station Finder App — Code Changes

### 3.1 Add Backend Retrofit Client

Replace direct OCM API calls with calls to the shared backend.

**New file: `data/network/BackendApi.kt`**
```kotlin
interface BackendApi {
    @GET("api/finder/stations/viewport")
    suspend fun getStationsInViewport(
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double
    ): Response<ApiResponse<List<StationMarker>>>

    @GET("api/finder/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 50.0,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<StationScore>>>

    @GET("api/finder/stations/{id}/detail")
    suspend fun getStationDetail(
        @Path("id") id: Long,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<ApiResponse<StationScore>>
}
```

**New file: `data/network/BackendClient.kt`**
```kotlin
object BackendClient {
    // URL configurable in local.properties or BuildConfig
    private const val BASE_URL = BuildConfig.BACKEND_URL
    
    val api: BackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { 
                    level = HttpLoggingInterceptor.Level.BODY 
                })
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
```

### 3.2 Add Backend API Response Models

**New file: `data/model/BackendModels.kt`**
```kotlin
// Generic wrapper matching backend's APIResponse
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// Map marker (lightweight)
data class StationMarker(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val available: Boolean
)

// Full station data
data class StationScore(
    val station: Station,
    val distance: Double,
    val score: Double,
    val availableSlots: Int,
    val totalSlots: Int,
    val connectorTypes: List<String>,
    val rating: Double
)

// Backend Station model
data class Station(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val operatingHours: String?,
    val pricePerKwh: Double?,
    val rating: Double?,
    val meta: String?,
    val isOpen: Boolean?
)
```

### 3.3 Update Repository

**Modified: `data/repository/StationRepository.kt`**
```kotlin
class StationRepository {
    private val backendApi = BackendClient.api
    
    suspend fun getNearbyStations(lat: Double, lng: Double, distance: Double = 20.0): List<StationScore> {
        return try {
            val response = backendApi.getNearbyStations(lat, lng, distance)
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching stations", e)
            emptyList()
        }
    }
    
    // ... similar for viewport and detail
}
```

### 3.4 Update ViewModel

**Modified: `StationViewModel.kt`**
- Keep the same `StationUiState` sealed class
- Change repository calls from `OCMStation` to `StationScore`/`StationMarker`
- Map `StationScore` to UI data (distance, available slots, etc.)

### 3.5 Update UI (StationDetailsSheet)

**Modified: `StationDetailsSheet.kt`**
- Accept `StationScore` instead of `OCMStation`
- Show:
  - Station name ✅
  - Operator (from `meta.ocm_operator` if available)
  - Address ✅
  - Connector types (from `connectorTypes` list)
  - Rating (stars visual)
  - Price per kWh
  - Operating hours
  - Available slots count
- Keep the "Navigate" (Google Maps) button

### 3.6 Update Screen (MainActivity)

**Modified: `MainActivity.kt` / `MapScreen.kt`**
- Minor changes to adapt from `OCMStation` to `StationScore`
- Keep the same map + bottom sheet UX
- Update type references only

### 3.7 Build Config

**Modified: `build.gradle.kts`**
- Add `BACKEND_URL` to `buildConfigField`
- Read from `local.properties` as `BACKEND_URL=http://10.0.2.2:8080/`

---

## 4. What Does NOT Change

### ❌ Main Android App (`android/`)
- **No file edited**
- `StationViewModel.kt` — unchanged  
- `ApiService.kt` — unchanged  
- `RetrofitClient.kt` — unchanged  
- `HomeScreen.kt` — unchanged  
- All booking/charging/payment screens — unchanged  
- `AndroidManifest.xml` — unchanged  

### ❌ Main Backend Auth Logic
- `SecurityConfig.java` — only **adds** `permitAll()` for `/api/finder/**`, doesn't modify existing rules
- `JwtRequestFilter.java` — unchanged
- `JwtUtil.java` — unchanged
- All existing controllers — unchanged

---

## 5. Data Model Comparison

| Info | OCM (Current) | Backend (New) | Station-Finder Shows |
|------|---------------|---------------|---------------------|
| Station name | ✅ `addressInfo.title` | ✅ `station.name` | ✅ Name |
| Operator | ✅ `operatorInfo.title` | ✅ `meta.ocm_operator` | ✅ Badge text |
| Address | ✅ `addressInfo.addressLine1` | ✅ `station.address` | ✅ Full address |
| Latitude/Longitude | ✅ `addressInfo.latitude/longitude` | ✅ `station.latitude/longitude` | ✅ Map markers |
| Connector types | ✅ `connections[].type.title` | ✅ `connectorTypes[]` | ✅ Chip list |
| Power (kW) | ✅ `connections[].powerKw` | — (available per slot) | ⚠️ Show from slot data |
| Operating status | ✅ `connections[].status.isOperational` | ✅ `station.isOpen` | ✅ Open/Closed badge |
| Rating | ❌ Not available | ✅ `station.rating` | ✅ Stars (new!) |
| Distance | ❌ Client computes | ✅ `stationScore.distance` | ✅ "X km away" |
| Available slots | ❌ Not available | ✅ `stationScore.availableSlots` | ✅ "3/6 available" |
| Price/kWh | ❌ Rarely available | ✅ `station.pricePerKwh` | ✅ "₹16.5/kWh" |
| Operating hours | ❌ Rarely available | ✅ `station.operatingHours` | ✅ "24 Hours" |
| Navigation | ✅ Lat/Lng → Google Maps | ✅ Lat/Lng → Google Maps | ✅ Same button |

**Net improvement:** The station-finder app gains **5 new data points** it didn't have before (rating, distance, available slots, price, operating hours) while losing nothing.

---

## 6. Implementation Phases

### Phase 1 — Backend Foundation (Estimated: 2-3 hours)

| Step | File | What |
|------|------|------|
| 1.1 | `StationImportService.java` | New service: fetch from OCM, transform, store, deduplicate |
| 1.2 | `StationFinderController.java` | New controller: finder endpoints delegating to existing services |
| 1.3 | `SecurityConfig.java` | Add `permitAll()` for `/api/finder/**` (1 line) |
| 1.4 | `StationSyncJob.java` | New scheduled job for periodic OCM sync |
| 1.5 | `application.properties` | Add `ocm.api.key`, `ocm.sync.*` config |

### Phase 2 — Backend Testing (Estimated: 1 hour)

| Step | What |
|------|------|
| 2.1 | Manual: Call `/api/finder/stations/nearby?lat=19.07&lng=72.87` |
| 2.2 | Manual: Call `/api/finder/stations/viewport?neLat=...&neLng=...&swLat=...&swLng=...` |
| 2.3 | Verify existing `/api/stations/...` endpoints still work (unchanged) |

### Phase 3 — Station Finder App Update (Estimated: 2-3 hours)

| Step | File | What |
|------|------|------|
| 3.1 | `data/network/BackendApi.kt` | New Retrofit interface |
| 3.2 | `data/network/BackendClient.kt` | New Retrofit client |
| 3.3 | `data/model/BackendModels.kt` | New response models |
| 3.4 | `data/repository/StationRepository.kt` | Modify to call backend |
| 3.5 | `StationViewModel.kt` | Update type references |
| 3.6 | `StationDetailsSheet.kt` | Update to show new data fields |
| 3.7 | `MainActivity.kt` / `MapScreen.kt` | Minor type updates |
| 3.8 | `build.gradle.kts` | Add BACKEND_URL build config |

### Phase 4 — Integration Test (Estimated: 1 hour)

| Step | What |
|------|------|
| 4.1 | Start backend with OCM API key configured |
| 4.2 | Run station-finder app pointing to local backend |
| 4.3 | Verify map shows stations from OCM |
| 4.4 | Tap station → verify bottom sheet with all data |
| 4.5 | Navigate to station → verify Google Maps opens |

---

## 7. Environment Configuration

### `local.properties` (station-finder)

```properties
# Keep existing keys
MAPS_API_KEY=your_google_maps_key
OCM_API_KEY=your_ocm_key

# NEW: Backend URL
# Use 10.0.2.2 for Android emulator (maps to host localhost)
# Use your machine's IP for physical device testing
BACKEND_URL=http://10.0.2.2:8080/
```

### `application.properties` (backend)

```properties
# NEW: OCM Import Configuration
ocm.api.key=${OCM_API_KEY:demo_key}
ocm.sync.enabled=true
ocm.sync.radius-km=500
ocm.sync.country-id=101
ocm.sync.interval-cron=0 0 3 * * ?
```

---

## 8. Edge Cases & Gotchas

| Issue | Solution |
|-------|----------|
| **OCM API rate limits** | Backend caches results in DB; sync is daily, not per-request |
| **Duplicate stations** | Dedup by OCM `UUID` stored in `meta` JSON field |
| **Station has no slots** | Create 1 default "Unknown" slot with status AVAILABLE |
| **Station-finder has no auth** | Public endpoints are truly anonymous — no token needed |
| **Backend is offline** | Station-finder should show error toast + retry button |
| **OCM data is sparse** | Backend's scored stations still work — missing fields show "N/A" |
| **GPS not available** | Fall back to default location (Mumbai: 19.0760, 72.8777) |
| **Main app's data** | Only main app's stations have owners/bookings; OCM stations won't |
| **Production deployment** | Backend must set `ocm.api.key` via env var, not in code |

---

## 9. Summary of Changes

### Backend (Additive — Nothing Removed)
- **3 new files**: `StationImportService.java`, `StationFinderController.java`, `StationSyncJob.java`
- **2 edited files**: `SecurityConfig.java` (+1 line), `application.properties` (+5 lines)
- **0 deleted files**

### Station Finder App (Replaces OCM Direct Calls)
- **3 new files**: `BackendApi.kt`, `BackendClient.kt`, `BackendModels.kt`
- **4 edited files**: `StationRepository.kt`, `StationViewModel.kt`, `StationDetailsSheet.kt`, `MainActivity.kt`, `build.gradle.kts`
- **0 deleted files** (can keep `OCMModels.kt` for future reference)

### Main Android App
- **0 files changed**

---

## 10. Future Enhancements (After This Plan)

| Feature | Benefit |
|---------|---------|
| Station photos from OCM/Google Places | App shows station images |
| User reviews (stored in backend) | Users rate stations they visit |
| Filter by connector type | "Show only CCS2 stations" |
| Favorite stations | Pin frequently used stations |
| Offline station cache | View last-seen stations without internet |
| Price filter | "Show stations under ₹20/kWh" |
