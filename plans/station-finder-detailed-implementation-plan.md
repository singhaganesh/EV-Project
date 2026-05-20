# Station Finder — Detailed Phase-by-Phase Implementation Plan

> **Goal:** Replace the station-finder Android app's direct OpenChargeMap API calls with the shared EV-Project backend, without touching the main Android app or breaking existing backend logic.
>
> **Branch:** Work on a new branch `feature/station-finder-backend`
>
> **Total estimated time:** ~8-12 hours

---

## Table of Contents

- [Phase 0: Setup & Preparation](#phase-0-setup--preparation)
- [Phase 1: Backend — StationFinderController](#phase-1-backend--stationfindercontroller)
- [Phase 2: Backend — StationImportService](#phase-2-backend--stationimportservice)
- [Phase 3: Backend — StationSyncJob & Configuration](#phase-3-backend--stationsyncjob--configuration)
- [Phase 4: Backend Testing](#phase-4-backend-testing)
- [Phase 5: Station Finder — New Network Layer](#phase-5-station-finder--new-network-layer)
- [Phase 6: Station Finder — Repository & ViewModel Updates](#phase-6-station-finder--repository--viewmodel-updates)
- [Phase 7: Station Finder — UI Updates (StationDetailsSheet)](#phase-7-station-finder--ui-updates-stationdetailssheet)
- [Phase 8: Integration Testing](#phase-8-integration-testing)
- [Appendix: Full File Reference](#appendix-full-file-reference)

---

## Phase 0: Setup & Preparation

### Step 0.1 — Create Git Branch

```bash
# From project root
cd D:\Ganesh\work\EV-Project
git checkout -b feature/station-finder-backend
```

### Step 0.2 — Verify Environment

```bash
# Check backend compiles
cd backend
./mvnw compile -q 2>&1 | tail -20

# Check station-finder compiles (if Gradle is available)
cd ../station-finder
./gradlew assembleDebug 2>&1 | tail -20
```

### Step 0.3 — Get an OCM API Key

1. Go to https://openchargemap.io/site/register
2. Sign up for a free account
3. Get your API key from the dashboard
4. Keep it ready — you'll need it in Phase 2

---

## Phase 1: Backend — StationFinderController

> **Files to create:** 1
> **Files to modify:** 1
> **Estimated time:** 30 min

### Step 1.1 — Create StationFinderController

**File path:** `backend/src/main/java/com/ganesh/EV_Project/controller/StationFinderController.java`

This controller exposes **public (no auth)** endpoints that the station-finder app calls. It delegates to existing `StationRecommendationService` and `StationService` — **zero logic duplication**.

```java
package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.StationMarkerDTO;
import com.ganesh.EV_Project.dto.StationScoreDTO;
import com.ganesh.EV_Project.dto.ViewportResponseDTO;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.StationRecommendationService;
import com.ganesh.EV_Project.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finder/stations")
public class StationFinderController {

    @Autowired
    private StationRecommendationService recommendationService;

    @Autowired
    private StationService stationService;

    /**
     * GET /api/finder/stations/nearby?lat=19.07&lng=72.87&radius=50&limit=20
     * Returns ranked nearby stations with full scoring data.
     */
    @GetMapping("/nearby")
    public ResponseEntity<APIResponse> getNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<StationScoreDTO> stations = recommendationService.getNearbyStationsRanked(lat, lng, radius, limit);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Found " + stations.size() + " nearby stations")
                    .data(stations)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    APIResponse.builder()
                            .success(false)
                            .message("Error fetching nearby stations: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/finder/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7
     * Returns lightweight markers for map pins in the current viewport.
     */
    @GetMapping("/viewport")
    public ResponseEntity<APIResponse> getStationsInViewport(
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam double swLat,
            @RequestParam double swLng) {
        try {
            List<StationMarkerDTO> markers = recommendationService.getStationsInViewport(swLat, neLat, swLng, neLng);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Found " + markers.size() + " stations in viewport")
                    .data(markers)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    APIResponse.builder()
                            .success(false)
                            .message("Error fetching viewport stations: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/finder/stations/detail/{id}?lat=19.07&lng=72.87
     * Returns full detail for a single station (on marker tap).
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<APIResponse> getStationDetail(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng) {
        try {
            StationScoreDTO detail = recommendationService.getStationDetail(id, lat, lng);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Station detail found")
                    .data(detail)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    APIResponse.builder()
                            .success(false)
                            .message("Error fetching station detail: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/finder/stations/search?q=nexon&lat=19.07&lng=72.87&radius=50
     * Text search by station name/address.
     */
    @GetMapping("/search")
    public ResponseEntity<APIResponse> searchStations(
            @RequestParam String q,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius) {
        try {
            List<StationScoreDTO> results = recommendationService.getNearbyStationsRanked(lat, lng, radius, 50);
            // Filter by name/address containing query (case-insensitive)
            String query = q.toLowerCase().trim();
            List<StationScoreDTO> filtered = results.stream()
                    .filter(s -> s.getStation().getName().toLowerCase().contains(query)
                            || s.getStation().getAddress().toLowerCase().contains(query))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Found " + filtered.size() + " stations matching \"" + q + "\"")
                    .data(filtered)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    APIResponse.builder()
                            .success(false)
                            .message("Error searching stations: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/finder/stations/count
     * Returns total number of stations in the system.
     */
    @GetMapping("/count")
    public ResponseEntity<APIResponse> getStationCount() {
        try {
            long count = stationService.getAllStations().size();
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Station count retrieved")
                    .data(Map.of("count", count))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    APIResponse.builder()
                            .success(false)
                            .message("Error counting stations: " + e.getMessage())
                            .build());
        }
    }
}
```

### Step 1.2 — Update SecurityConfig

**File path:** `backend/src/main/java/com/ganesh/EV_Project/config/SecurityConfig.java`

Add one line after the existing `.permitAll()` calls:

```java
// Inside the authorizeHttpRequests lambda, after:
auth.requestMatchers("/api/public/**").permitAll();

// ADD THIS LINE:
auth.requestMatchers("/api/finder/**").permitAll();
```

**Context in the file (around line 76):**
```java
// Public System Endpoints
auth.requestMatchers("/api/public/**").permitAll();
auth.requestMatchers("/api/finder/**").permitAll();   // <-- ADD THIS
auth.requestMatchers("/ws/**").permitAll();
```

### ✅ Phase 1 Verification

Build the backend to make sure it compiles:

```bash
cd backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 2: Backend — StationImportService

> **Files to create:** 1
> **Files to modify:** 0
> **Estimated time:** 1-2 hours

### Step 2.1 — Create StationImportService

**File path:** `backend/src/main/java/com/ganesh/EV_Project/service/StationImportService.java`

This service fetches station data from the OpenChargeMap API and stores it in the local PostgreSQL database. It handles:
- Fetching from OCM API
- Transforming OCM data → backend `Station` model
- Creating default `ChargerSlot` entries per connector
- Deduplication by OCM UUID
- Rate-limit aware (sync is async, not per-request)

```java
package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class StationImportService {

    private static final Logger log = LoggerFactory.getLogger(StationImportService.class);

    @Value("${ocm.api.key}")
    private String ocmApiKey;

    @Value("${ocm.sync.radius-km:500}")
    private int defaultRadiusKm;

    @Value("${ocm.sync.country-id:101}")
    private int countryId;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerSlotRepository chargerSlotRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch stations from OCM within a radius of a center point.
     * 
     * @param centerLat  Center latitude
     * @param centerLng  Center longitude
     * @param radiusKm   Search radius in kilometers
     * @return Number of stations imported
     */
    @Transactional
    public int importFromOCM(double centerLat, double centerLng, int radiusKm) {
        int imported = 0;
        int skipped = 0;

        try {
            String url = buildOCMUrl(centerLat, centerLng, radiusKm);
            log.info("Fetching OCM stations from: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("OCM API returned status: {}", response.getStatusCode());
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray()) {
                log.error("OCM response is not an array");
                return 0;
            }

            for (JsonNode node : root) {
                try {
                    if (transformAndSave(node)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to import OCM station: {}", e.getMessage());
                    skipped++;
                }
            }

            log.info("OCM import complete: {} imported, {} skipped (total {})", imported, skipped, root.size());

        } catch (Exception e) {
            log.error("OCM import failed: {}", e.getMessage(), e);
        }

        return imported;
    }

    /**
     * Build the OCM API URL with query parameters.
     */
    private String buildOCMUrl(double lat, double lng, int radiusKm) {
        String baseUrl = "https://api.openchargemap.io/v3/poi";
        return baseUrl + "?key=" + URLEncoder.encode(ocmApiKey, StandardCharsets.UTF_8)
                + "&latitude=" + lat
                + "&longitude=" + lng
                + "&distance=" + radiusKm
                + "&distanceunit=KM"
                + "&maxresults=100"
                + "&compact=true"
                + "&verbose=false"
                + "&countryid=" + countryId;
    }

    /**
     * Transform a single OCM station JSON node into a Station entity and save it.
     * Returns true if a new station was created, false if skipped (duplicate or invalid).
     */
    private boolean transformAndSave(JsonNode ocmNode) {
        // Extract OCM UUID for deduplication
        String ocmUuid = ocmNode.has("UUID") && !ocmNode.get("UUID").isNull()
                ? ocmNode.get("UUID").asText()
                : null;

        if (ocmUuid == null || ocmUuid.isEmpty()) {
            return false; // Skip stations without UUID
        }

        // Check if this station was already imported (dedup by OCM UUID in meta)
        boolean exists = stationRepository.findAll().stream()
                .anyMatch(s -> s.getMeta() != null && s.getMeta().contains("\"ocm_id\":" + ocmNode.get("ID").asLong()));

        if (exists) {
            return false; // Skip — already imported
        }

        // Extract AddressInfo
        JsonNode addressInfo = ocmNode.get("AddressInfo");
        if (addressInfo == null) {
            return false;
        }

        String name = addressInfo.has("Title") ? addressInfo.get("Title").asText() : "Unknown Station";
        double latitude = addressInfo.has("Latitude") ? addressInfo.get("Latitude").asDouble() : 0;
        double longitude = addressInfo.has("Longitude") ? addressInfo.get("Longitude").asDouble() : 0;

        if (latitude == 0 && longitude == 0) {
            return false; // Skip stations with no coordinates
        }

        // Build address string
        StringBuilder addressBuilder = new StringBuilder();
        if (addressInfo.has("AddressLine1") && !addressInfo.get("AddressLine1").isNull()) {
            addressBuilder.append(addressInfo.get("AddressLine1").asText());
        }
        if (addressInfo.has("Town") && !addressInfo.get("Town").isNull()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(addressInfo.get("Town").asText());
        }
        if (addressInfo.has("StateOrProvince") && !addressInfo.get("StateOrProvince").isNull()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(addressInfo.get("StateOrProvince").asText());
        }
        String address = addressBuilder.length() > 0 ? addressBuilder.toString() : "Address not available";

        // Build meta JSON
        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append("{");
        metaBuilder.append("\"ocm_id\":").append(ocmNode.get("ID").asLong());
        metaBuilder.append(",\"ocm_uuid\":\"").append(escapeJson(ocmUuid)).append("\"");

        // Operator info
        if (ocmNode.has("OperatorInfo") && !ocmNode.get("OperatorInfo").isNull()) {
            JsonNode operator = ocmNode.get("OperatorInfo");
            if (operator.has("Title") && !operator.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_operator\":\"").append(escapeJson(operator.get("Title").asText())).append("\"");
            }
            if (operator.has("WebsiteURL") && !operator.get("WebsiteURL").isNull()) {
                metaBuilder.append(",\"ocm_website\":\"").append(escapeJson(operator.get("WebsiteURL").asText())).append("\"");
            }
        }

        // Usage type
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            if (usage.has("Title") && !usage.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_usage_type\":\"").append(escapeJson(usage.get("Title").asText())).append("\"");
            }
        }

        // Comments
        if (ocmNode.has("GeneralComments") && !ocmNode.get("GeneralComments").isNull()) {
            metaBuilder.append(",\"ocm_comments\":\"").append(escapeJson(ocmNode.get("GeneralComments").asText())).append("\"");
        }

        metaBuilder.append(",\"source\":\"OCM\"");
        metaBuilder.append(",\"last_synced\":\"").append(java.time.LocalDateTime.now().toString()).append("\"");
        metaBuilder.append("}");

        // Operating hours from OCM (if available)
        String operatingHours = "24 Hours"; // Default
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            // Some common OCM usage types that imply 24/7
            String usageTitle = usage.has("Title") ? usage.get("Title").asText() : "";
            if (usageTitle.toLowerCase().contains("pay") || usageTitle.toLowerCase().contains("public")) {
                operatingHours = "24 Hours";
            }
        }

        // Create Station entity
        Station station = Station.builder()
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .address(address)
                .owner(null) // No owner — these are public OCM stations
                .meta(metaBuilder.toString())
                .rating(0.0)
                .operatingHours(operatingHours)
                .pricePerKwh(0.0) // Unknown from OCM; owner can set later
                .truckPricePerKwh(0.0)
                .build();

        station = stationRepository.save(station);

        // Create ChargerSlots from OCM connection data
        List<ChargerSlot> slotsToCreate = new ArrayList<>();

        if (ocmNode.has("Connections") && ocmNode.get("Connections").isArray()) {
            for (JsonNode conn : ocmNode.get("Connections")) {
                int quantity = conn.has("Quantity") && !conn.get("Quantity").isNull()
                        ? conn.get("Quantity").asInt()
                        : 1;

                double powerKw = conn.has("PowerKW") && !conn.get("PowerKW").isNull()
                        ? conn.get("PowerKW").asDouble()
                        : 22.0;

                ConnectorType connectorType = mapConnectorType(conn);
                SlotType slotType = (connectorType == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;

                for (int i = 0; i < quantity; i++) {
                    ChargerSlot slot = ChargerSlot.builder()
                            .station(station)
                            .dispensary(null) // OCM doesn't provide dispensary data
                            .slotLabel(connectorType.toString() + " #" + (i + 1))
                            .slotType(slotType)
                            .status(SlotStatus.AVAILABLE)
                            .connectorType(connectorType)
                            .powerKw(powerKw)
                            .build();
                    slotsToCreate.add(slot);
                }
            }
        }

        // If no connections found, create 2 default CCS2 slots
        if (slotsToCreate.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                ChargerSlot slot = ChargerSlot.builder()
                        .station(station)
                        .dispensary(null)
                        .slotLabel("CCS2 #" + (i + 1))
                        .slotType(SlotType.DC)
                        .status(SlotStatus.AVAILABLE)
                        .connectorType(ConnectorType.CCS2)
                        .powerKw(60.0)
                        .build();
                slotsToCreate.add(slot);
            }
        }

        chargerSlotRepository.saveAll(slotsToCreate);
        log.info("Imported station: {} ({} slots)", name, slotsToCreate.size());
        return true;
    }

    /**
     * Map OCM connection type to backend ConnectorType enum.
     */
    private ConnectorType mapConnectorType(JsonNode connection) {
        if (connection.has("ConnectionType") && !connection.get("ConnectionType").isNull()) {
            JsonNode connType = connection.get("ConnectionType");
            String title = connType.has("Title") && !connType.get("Title").isNull()
                    ? connType.get("Title").asText().toLowerCase()
                    : "";

            if (title.contains("ccs") || title.contains("combo") || title.contains("dc")) {
                return ConnectorType.CCS2;
            }
            if (title.contains("type 2") || title.contains("type2") || title.contains("mennekes")) {
                return ConnectorType.TYPE_2;
            }
            if (title.contains("chademo")) {
                return ConnectorType.CCS2; // Treat CHAdeMO as CCS2 for compatibility
            }
        }

        // Check power level: high power = DC
        if (connection.has("PowerKW") && !connection.get("PowerKW").isNull()) {
            double power = connection.get("PowerKW").asDouble();
            if (power > 22) {
                return ConnectorType.CCS2;
            }
        }

        return ConnectorType.CCS2; // Default
    }

    /**
     * Escape a string for safe inclusion in JSON.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

### ✅ Phase 2 Verification

```bash
cd backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 3: Backend — StationSyncJob & Configuration

> **Files to create:** 1
> **Files to modify:** 2
> **Estimated time:** 30 min

### Step 3.1 — Create StationSyncJob

**File path:** `backend/src/main/java/com/ganesh/EV_Project/config/StationSyncJob.java`

A scheduled job that periodically syncs new/updated stations from OpenChargeMap.

```java
package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "ocm.sync.enabled", havingValue = "true", matchIfMissing = false)
public class StationSyncJob {

    private static final Logger log = LoggerFactory.getLogger(StationSyncJob.class);

    @Autowired
    private StationImportService stationImportService;

    @Value("${ocm.sync.radius-km:500}")
    private int radiusKm;

    @Value("${ocm.sync.country-id:101}")
    private int countryId;

    /**
     * Daily sync at 3:00 AM.
     * Uses default center point (Mumbai) as the anchor for importing nearby stations.
     * In production, you can make this smarter by syncing multiple regions.
     */
    @Scheduled(cron = "${ocm.sync.interval-cron:0 0 3 * * ?}")
    public void syncStations() {
        log.info("Starting scheduled OCM station sync...");

        // Mumbai region
        int mumbaiCount = stationImportService.importFromOCM(19.0760, 72.8777, radiusKm);
        log.info("Mumbai region: {} stations imported", mumbaiCount);

        // Bangalore region
        int bangaloreCount = stationImportService.importFromOCM(12.9716, 77.5946, radiusKm);
        log.info("Bangalore region: {} stations imported", bangaloreCount);

        // Delhi region
        int delhiCount = stationImportService.importFromOCM(28.7041, 77.1025, radiusKm);
        log.info("Delhi region: {} stations imported", delhiCount);

        // Hyderabad region
        int hyderabadCount = stationImportService.importFromOCM(17.3850, 78.4867, radiusKm);
        log.info("Hyderabad region: {} stations imported", hyderabadCount);

        // Chennai region
        int chennaiCount = stationImportService.importFromOCM(13.0827, 80.2707, radiusKm);
        log.info("Chennai region: {} stations imported", chennaiCount);

        int total = mumbaiCount + bangaloreCount + delhiCount + hyderabadCount + chennaiCount;
        log.info("Scheduled OCM sync complete. Total new stations: {}", total);
    }
}
```

### Step 3.2 — Update DataSeeder (Optional Enhancement)

**File path:** `backend/src/main/java/com/ganesh/EV_Project/config/DataSeeder.java`

Add OCM import logic to seed real stations on first startup. Insert this at the end of the `run()` method (after existing seeding logic):

```java
// ====================================
// 🗺️ OCM STATION IMPORT (First run only)
// ====================================
if (stationRepository.count() <= 3) { // Rough check for "no real data"
    log.info("🗺️ SEEDING STATIONS FROM OPENCHARGEMAP");
    try {
        int mumbaiCount = stationImportService.importFromOCM(19.0760, 72.8777, 50);
        log.info("Imported {} stations from Mumbai region", mumbaiCount);

        int bangaloreCount = stationImportService.importFromOCM(12.9716, 77.5946, 50);
        log.info("Imported {} stations from Bangalore region", bangaloreCount);
    } catch (Exception e) {
        log.warn("OCM seeding skipped (API key may not be configured): {}", e.getMessage());
    }
}
```

**Important:** You'll need to inject `StationImportService` and `StationRepository` into `DataSeeder.java`:

```java
@Autowired
private StationImportService stationImportService;

@Autowired
private StationRepository stationRepository;
```

### Step 3.3 — Update application.properties

**File path:** `backend/src/main/resources/application.properties`

Add these lines at the end of the file:

```properties
# ========================================
# OCM (OpenChargeMap) Import Configuration
# ========================================
ocm.api.key=${OCM_API_KEY:demo_key}
ocm.sync.enabled=true
ocm.sync.radius-km=500
ocm.sync.country-id=101
ocm.sync.interval-cron=0 0 3 * * ?
```

### ✅ Phase 3 Verification

```bash
cd backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 4: Backend Testing

> **Estimated time:** 1 hour

### Step 4.1 — Start Backend with OCM API Key

```bash
cd backend
# Set your OCM API key (replace with your actual key)
export OCM_API_KEY="your_ocm_api_key_here"
# Or on Windows (cmd):
# set OCM_API_KEY=your_ocm_api_key_here
# Or on Windows (PowerShell):
# $env:OCM_API_KEY="your_ocm_api_key_here"

./mvnw spring-boot:run
```

### Step 4.2 — Test Finder Endpoints

Open a new terminal and test each endpoint:

```bash
# 1. Test nearby stations
curl "http://localhost:8080/api/finder/stations/nearby?lat=19.0760&lng=72.8777&radius=50&limit=5"

# 2. Test viewport markers
curl "http://localhost:8080/api/finder/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7"

# 3. Test count
curl "http://localhost:8080/api/finder/stations/count"

# 4. Test search
curl "http://localhost:8080/api/finder/stations/search?q=nexon&lat=19.0760&lng=72.8777"

# 5. Test detail (replace {id} with an actual station ID from step 1)
curl "http://localhost:8080/api/finder/stations/detail/1?lat=19.0760&lng=72.8777"
```

### Step 4.3 — Verify Main App Endpoints Still Work

```bash
# GET stations (public endpoint for main app too)
curl "http://localhost:8080/api/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7"

# Login with test credentials
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"phone":"9999999999","otp":"000000"}'

# Verify auth-protected endpoints still require token
curl "http://localhost:8080/api/bookings"  # Should return 403
```

### Expected Results

| Test | Expected Status | Expected Response |
|------|----------------|-------------------|
| `nearby` | 200 | JSON with `success: true`, array of stations |
| `viewport` | 200 | JSON with `success: true`, array of markers |
| `count` | 200 | JSON with `data.count` = number |
| `search` | 200 | Filtered results |
| `detail` | 200 | Single station with full scoring |
| Main app endpoints | 200 (GET) / 403 (POST without auth) | Unchanged behavior |

### Troubleshooting

- **OCM API returns empty**: Check the API key is valid and `country-id=101` (India) is correct
- **Stations not imported**: Check backend logs for `OCM import complete` or error messages
- **`/api/finder/` returns 401**: Make sure the `permitAll()` line was added in `SecurityConfig.java`

---

## Phase 5: Station Finder — New Network Layer

> **Files to create:** 2
> **Files to modify:** 1 (build.gradle.kts)
> **Estimated time:** 45 min

This phase replaces the direct OCM API calls with calls to the shared backend.

### Step 5.1 — Create BackendApi.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/network/BackendApi.kt`

```kotlin
package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.data.model.BackendStationMarker
import com.ganesh.stationfinder.data.model.BackendStationScore
import com.ganesh.stationfinder.data.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApi {

    @GET("api/finder/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 50.0,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<BackendStationScore>>

    @GET("api/finder/stations/viewport")
    suspend fun getStationsInViewport(
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double
    ): ApiResponse<List<BackendStationMarker>>

    @GET("api/finder/stations/detail/{id}")
    suspend fun getStationDetail(
        @Path("id") id: Long,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): ApiResponse<BackendStationScore>

    @GET("api/finder/stations/search")
    suspend fun searchStations(
        @Query("q") query: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 50.0
    ): ApiResponse<List<BackendStationScore>>

    @GET("api/finder/stations/count")
    suspend fun getStationCount(): ApiResponse<Map<String, Any>>
}
```

### Step 5.2 — Create BackendClient.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/network/BackendClient.kt`

```kotlin
package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BackendClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val api: BackendApi = retrofit.create(BackendApi::class.java)
}
```

### Step 5.3 — Create BackendModels.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/model/BackendModels.kt`

```kotlin
package com.ganesh.stationfinder.data.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching backend's APIResponse.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

/**
 * Lightweight marker for map pins (from GET /api/finder/stations/viewport).
 */
data class BackendStationMarker(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("available") val available: Boolean
)

/**
 * Full station score DTO (from GET /api/finder/stations/nearby and detail).
 */
data class BackendStationScore(
    @SerializedName("station") val station: BackendStation,
    @SerializedName("distance") val distance: Double?,
    @SerializedName("score") val score: Double?,
    @SerializedName("trafficScore") val trafficScore: Double?,
    @SerializedName("gridScore") val gridScore: Double?,
    @SerializedName("parkingScore") val parkingScore: Double?,
    @SerializedName("accessScore") val accessScore: Double?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    @SerializedName("totalSlots") val totalSlots: Int?,
    @SerializedName("connectorTypes") val connectorTypes: List<String>?,
    @SerializedName("lastActive") val lastActive: String?,
    @SerializedName("rating") val rating: Double?
)

/**
 * Backend Station model.
 */
data class BackendStation(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String,
    @SerializedName("operatingHours") val operatingHours: String?,
    @SerializedName("pricePerKwh") val pricePerKwh: Double?,
    @SerializedName("truckPricePerKwh") val truckPricePerKwh: Double?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("meta") val meta: String?,
    @SerializedName("isOpen") val isOpen: Boolean?,
    @SerializedName("lastUsedTime") val lastUsedTime: String?
)
```

### Step 5.4 — Update build.gradle.kts (Add BACKEND_URL)

**File path:** `station-finder/app/build.gradle.kts`

Add `BACKEND_URL` to the `buildConfigField` section:

```kotlin
// In the defaultConfig block, after the existing buildConfigField for OCM_API_KEY:
defaultConfig {
    // ... existing config ...

    manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    buildConfigField("String", "OCM_API_KEY", "\"${ocmApiKey}\"")
    buildConfigField("String", "BACKEND_URL", "\"${backendUrl}\"")  // <-- ADD THIS
}
```

And add the `backendUrl` property at the top with the other properties:

```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")?.trim() ?: ""
val ocmApiKey: String = localProperties.getProperty("OCM_API_KEY")?.trim() ?: ""
val backendUrl: String = localProperties.getProperty("BACKEND_URL")?.trim() ?: "http://10.0.2.2:8080/"
```

### Step 5.5 — Update local.properties (in station-finder)

**File path:** `station-finder/local.properties`

Add the backend URL:

```properties
MAPS_API_KEY=your_google_maps_key
OCM_API_KEY=your_ocm_key
BACKEND_URL=http://10.0.2.2:8080/
```

> **Note:** Use `10.0.2.2` for Android emulator (maps to host machine's localhost). Use your machine's actual IP (e.g., `http://192.168.1.100:8080/`) for physical device testing.

### ✅ Phase 5 Verification

```bash
cd station-finder
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

## Phase 6: Station Finder — Repository & ViewModel Updates

> **Files to modify:** 2
> **Estimated time:** 30 min

### Step 6.1 — Update StationRepository.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/repository/StationRepository.kt`

Replace the OCM API calls with backend API calls. The models change from `OCMStation` to `BackendStationScore`/`BackendStationMarker`.

```kotlin
package com.ganesh.stationfinder.data.repository

import android.util.Log
import com.ganesh.stationfinder.data.model.BackendStationMarker
import com.ganesh.stationfinder.data.model.BackendStationScore
import com.ganesh.stationfinder.data.network.BackendClient

class StationRepository {

    private val api = BackendClient.api

    /**
     * Fetch nearby stations from the shared backend.
     */
    suspend fun getNearbyStations(lat: Double, lng: Double, distance: Double = 20.0): List<BackendStationScore> {
        Log.d("Repository", "Fetching nearby stations at Lat: $lat, Lng: $lng (Radius: ${distance}km)")
        return try {
            val response = api.getNearbyStations(lat, lng, distance)
            if (response.success && response.data != null) {
                Log.d("Repository", "Backend returned ${response.data.size} stations")
                response.data
            } else {
                Log.w("Repository", "Backend returned error: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching stations from backend", e)
            emptyList()
        }
    }

    /**
     * Fetch stations in a viewport (for map markers).
     */
    suspend fun getStationsInViewport(
        neLat: Double, neLng: Double,
        swLat: Double, swLng: Double
    ): List<BackendStationMarker> {
        return try {
            val response = api.getStationsInViewport(neLat, neLng, swLat, swLng)
            if (response.success && response.data != null) {
                response.data
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching viewport stations", e)
            emptyList()
        }
    }

    /**
     * Fetch detail for a single station.
     */
    suspend fun getStationDetail(id: Long, lat: Double, lng: Double): BackendStationScore? {
        return try {
            val response = api.getStationDetail(id, lat, lng)
            if (response.success && response.data != null) {
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching station detail", e)
            null
        }
    }

    /**
     * Search stations by name/address.
     */
    suspend fun searchStations(query: String, lat: Double, lng: Double, radius: Double = 50.0): List<BackendStationScore> {
        return try {
            val response = api.searchStations(query, lat, lng, radius)
            if (response.success && response.data != null) {
                response.data
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error searching stations", e)
            emptyList()
        }
    }

    /**
     * Get total station count.
     */
    suspend fun getStationCount(): Int {
        return try {
            val response = api.getStationCount()
            if (response.success && response.data != null) {
                (response.data["count"] as? Double)?.toInt() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching station count", e)
            0
        }
    }
}
```

### Step 6.2 — Update StationViewModel.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/StationViewModel.kt`

Change the generic type parameter in `StationUiState` from `OCMStation` to `BackendStationScore`. The debounce and zoom logic stays the same.

```kotlin
package com.ganesh.stationfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.stationfinder.data.model.BackendStationScore
import com.ganesh.stationfinder.data.repository.StationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed class StationUiState {
    object Loading : StationUiState()
    data class Success(val stations: List<BackendStationScore>) : StationUiState()  // <-- TYPE CHANGED
    data class Error(val message: String) : StationUiState()
}

class StationViewModel : ViewModel() {
    private val repository = StationRepository()
    private var searchJob: Job? = null
    private var lastFetchedLocation: LatLng? = null

    private val _uiState = MutableStateFlow<StationUiState>(StationUiState.Loading)
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

    fun fetchNearbyStations(location: LatLng, distance: Double = 20.0) {
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _uiState.value = StationUiState.Loading
            try {
                val stations = repository.getNearbyStations(location.latitude, location.longitude, distance)
                _uiState.value = StationUiState.Success(stations)
                lastFetchedLocation = location
            } catch (e: Exception) {
                _uiState.value = StationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchNearbyStationsDebounced(location: LatLng, zoom: Float) {
        val distanceMoved = lastFetchedLocation?.let {
            calculateDistance(it, location)
        } ?: Float.MAX_VALUE

        if (distanceMoved < 500f) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(1500)
            val radius = calculateRadiusFromZoom(zoom)
            fetchNearbyStations(location, radius)
        }
    }

    private fun calculateRadiusFromZoom(zoom: Float): Double {
        return when {
            zoom >= 15f -> 5.0
            zoom >= 12f -> 15.0
            zoom >= 10f -> 30.0
            else -> 50.0
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0]
    }
}
```

### ✅ Phase 6 Verification

```bash
cd station-finder
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

## Phase 7: Station Finder — UI Updates (StationDetailsSheet + MainActivity)

> **Files to modify:** 2
> **Estimated time:** 1 hour

### Step 7.1 — Rewrite StationDetailsSheet.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/StationDetailsSheet.kt`

The main change is accepting `BackendStationScore` instead of `OCMStation`, and displaying the **new data fields** (rating, distance, available slots, price, operating hours).

```kotlin
package com.ganesh.stationfinder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.stationfinder.data.model.BackendStationScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailsSheet(
    stationScore: BackendStationScore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val station = stationScore.station

    // Parse meta JSON for operator name
    val operatorName = try {
        station.meta?.let { meta ->
            val regex = "\"ocm_operator\":\"([^\"]+)\"".toRegex()
            regex.find(meta)?.groupValues?.getOrNull(1)
        }
    } catch (e: Exception) { null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // ── Row 1: Icon + Name + Rating ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE0F7F9), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EvStation, null, tint = Color(0xFF00BCD4))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = operatorName ?: "EV Charging Station",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        // Rating stars
                        val rating = stationScore.rating ?: station.rating ?: 0.0
                        if (rating > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "★",
                                color = Color(0xFFFFB300),
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Row 2: Distance + Available Slots + Price ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Distance
                InfoChip(
                    icon = Icons.Default.NearMe,
                    value = stationScore.distance?.let {
                        String.format("%.1f km", it)
                    } ?: "N/A",
                    label = "Distance"
                )
                // Available Slots
                InfoChip(
                    icon = Icons.Default.EvStation,
                    value = "${stationScore.availableSlots ?: 0}/${stationScore.totalSlots ?: 0}",
                    label = "Available"
                )
                // Price
                InfoChip(
                    icon = Icons.Default.CurrencyRupee,
                    value = station.pricePerKwh?.let {
                        "₹${String.format("%.1f", it)}/kWh"
                    } ?: "N/A",
                    label = "Price"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Row 3: Address ──
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // Operating hours
                    station.operatingHours?.let { hours ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (station.isOpen == true) Color(0xFF059669) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hours,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (station.isOpen == true) Color(0xFF059669) else Color.Gray
                            )
                            if (station.isOpen == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "● Open",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Row 4: Connector Types ──
            Text(
                text = "Connectors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!stationScore.connectorTypes.isNullOrEmpty()) {
                stationScore.connectorTypes.forEach { connType ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = connType,
                                fontWeight = FontWeight.SemiBold
                            )
                            val isDC = connType.contains("CCS", ignoreCase = true)
                            Text(
                                text = if (isDC) "⚡ DC Fast" else "🔌 AC",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDC) Color(0xFF059669) else Color(0xFF0288D1)
                            )
                        }
                    }
                }
            } else {
                Text("Connector information not available", color = Color.Gray)
            }

            // Last active info
            stationScore.lastActive?.let { lastActive ->
                if (lastActive != "Never") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last active: $lastActive",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Navigate Button ──
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2234))
            ) {
                Icon(Icons.Default.Directions, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF1A2234))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
```

### Step 7.2 — Update MainActivity.kt

**File path:** `station-finder/app/src/main/java/com/ganesh/stationfinder/MainActivity.kt`

Changes needed:
1. Import `BackendStationScore` instead of `OCMStation`
2. Change the `selectedStation` type from `OCMStation?` to `BackendStationScore?`
3. Update marker rendering to use `BackendStationScore.station` fields
4. Pass `stationScore` to `StationDetailsSheet` instead of `station`

```kotlin
package com.ganesh.stationfinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.stationfinder.data.model.BackendStationScore  // <-- NEW IMPORT
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.maps.compose.*
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen()
                }
            }
        }
    }
}

@Composable
fun MapScreen(viewModel: StationViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedStation by remember { mutableStateOf<BackendStationScore?>(null) }  // <-- TYPE CHANGED

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            com.ganesh.stationfinder.util.LocationHelper.getCurrentLocation(context) { location ->
                userLocation = location
                location?.let { viewModel.fetchNearbyStations(it) }
            }
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is StationUiState.Error -> {
                android.widget.Toast.makeText(
                    context,
                    "Error: ${(uiState as StationUiState.Error).message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            is StationUiState.Success -> {
                if ((uiState as StationUiState.Success).stations.isEmpty()) {
                    android.widget.Toast.makeText(
                        context,
                        "No stations found in this area",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userLocation != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLocation!!, 12f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                if (uiState is StationUiState.Success) {
                    val stationScores = (uiState as StationUiState.Success).stations
                    stationScores.forEach { stationScore ->
                        val station = stationScore.station
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    station.latitude,
                                    station.longitude
                                )
                            ),
                            title = station.name,
                            snippet = stationScore.distance?.let {
                                String.format("%.1f km", it)
                            } ?: "",
                            onClick = {
                                selectedStation = stationScore  // <-- TYPE UPDATED
                                true
                            }
                        )
                    }
                }
            }

            // Trigger fetch when camera stops
            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    val center = cameraPositionState.position.target
                    val zoom = cameraPositionState.position.zoom
                    viewModel.fetchNearbyStationsDebounced(center, zoom)
                }
            }

            // Top Search Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Button(
                    onClick = {
                        val center = cameraPositionState.position.target
                        viewModel.fetchNearbyStations(center)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search this area", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching your location...", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Selected Station Details Sheet — Now passes stationScore
        selectedStation?.let { stationScore ->
            StationDetailsSheet(
                stationScore = stationScore,  // <-- PARAMETER CHANGED
                onDismiss = { selectedStation = null }
            )
        }

        // Loading indicator overlay
        if (uiState is StationUiState.Loading && userLocation != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = Color.Blue
            )
        }
    }
}
```

### ✅ Phase 7 Verification

```bash
cd station-finder
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

## Phase 8: Integration Testing

> **Estimated time:** 1 hour

### Step 8.1 — Start Backend

```bash
cd backend
export OCM_API_KEY="your_ocm_api_key_here"
./mvnw spring-boot:run
```

Wait for: `Started EvProjectApplication in X.XXX seconds`

### Step 8.2 — Configure Station Finder for Emulator

Edit `station-finder/local.properties`:
```properties
BACKEND_URL=http://10.0.2.2:8080/
```

### Step 8.3 — Build and Install Station Finder

```bash
cd station-finder
./gradlew installDebug
```

### Step 8.4 — Manual Test Checklist

| # | Test Case | Steps | Expected |
|---|-----------|-------|----------|
| 1 | **App launches** | Open the app | Shows map centered on your location |
| 2 | **Stations appear** | Wait for "Search this area" → tap it | Station markers appear on the map |
| 3 | **Station detail** | Tap any marker | Bottom sheet opens with name, rating, distance, slots, price, connectors |
| 4 | **Missing data** | Tap a station with `N/A` fields | Shows "N/A" gracefully |
| 5 | **Navigate** | Tap "Start Navigation" | Google Maps opens with navigation |
| 6 | **Open/Closed** | Check a station's operating hours badge | Shows "● Open" (green) or hours in gray |
| 7 | **Search area** | Pan map, tap "Search this area" | New markers load for the new area |
| 8 | **No stations** | Pan to remote area, search | Shows "No stations found" toast |

### Step 8.5 — Verify Main App Still Works

| # | Test | How |
|---|------|-----|
| 1 | Main EV app launches | Build and install the main `android/` app |
| 2 | Login works | Login with OTP |
| 3 | Station map works | Home screen map shows stations |
| 4 | No regression | All existing features work as before |

---

## Rollback Plan

If something goes wrong during any phase, rollback is simple because all backend changes are **additive**:

### Backend Rollback
```bash
cd backend
git checkout -- src/main/java/com/ganesh/EV_Project/controller/StationFinderController.java
git checkout -- src/main/java/com/ganesh/EV_Project/service/StationImportService.java
git checkout -- src/main/java/com/ganesh/EV_Project/config/StationSyncJob.java
git checkout -- src/main/java/com/ganesh/EV_Project/config/SecurityConfig.java
git checkout -- src/main/resources/application.properties
```

### Station Finder Rollback
```bash
cd station-finder
git checkout -- .
```

---

## Appendix: Full File Reference

### Files Created

| # | File | Phase | Purpose |
|---|------|-------|---------|
| 1 | `backend/.../controller/StationFinderController.java` | 1 | Public finder endpoints |
| 2 | `backend/.../service/StationImportService.java` | 2 | OCM data import |
| 3 | `backend/.../config/StationSyncJob.java` | 3 | Scheduled OCM sync |
| 4 | `station-finder/.../network/BackendApi.kt` | 5 | Retrofit interface |
| 5 | `station-finder/.../network/BackendClient.kt` | 5 | Retrofit client |
| 6 | `station-finder/.../model/BackendModels.kt` | 5 | Backend response models |

### Files Modified

| # | File | Phase | Change |
|---|------|-------|--------|
| 1 | `backend/.../config/SecurityConfig.java` | 1 | Add `permitAll()` for `/api/finder/**` |
| 2 | `backend/.../config/DataSeeder.java` | 3 | Optional OCM seed on startup |
| 3 | `backend/src/main/resources/application.properties` | 3 | Add OCM config |
| 4 | `station-finder/app/build.gradle.kts` | 5 | Add `BACKEND_URL` build config |
| 5 | `station-finder/.../repository/StationRepository.kt` | 6 | Replace OCM calls with backend calls |
| 6 | `station-finder/.../StationViewModel.kt` | 6 | Change types to `BackendStationScore` |
| 7 | `station-finder/.../StationDetailsSheet.kt` | 7 | Accept `BackendStationScore`, show new fields |
| 8 | `station-finder/.../MainActivity.kt` | 7 | Update types and marker rendering |

### Files NOT Changed (Zero Disruption)

All of these remain **untouched**:

**Backend (existing auth logic):**
- `JwtRequestFilter.java` — unchanged
- `JwtUtil.java` — unchanged
- `UserDetailsServiceImpl.java` — unchanged
- All existing controllers — unchanged
- All existing services — unchanged

**Main Android App:**
- `android/app/src/main/java/com/ganesh/ev/` — ALL files unchanged
- `StationViewModel.kt` — unchanged
- `ApiService.kt` — unchanged
- `RetrofitClient.kt` — unchanged
- All screens — unchanged

---

## Summary of What Each Phase Delivers

| Phase | Delivers | You Can Demo After |
|-------|----------|-------------------|
| 1 | Backend endpoints ready (no data yet) | `curl` shows endpoints work but return empty |
| 2 | OCM import service | Import stations from OCM on demand |
| 3 | Auto-sync + config | Stations automatically seeded via DataSeeder |
| 4 | Backend fully tested | `curl` shows real stations from OCM |
| 5 | Station finder can talk to backend | App builds with new API client |
| 6 | Data flowing through ViewModel | App shows station markers from backend |
| 7 | Full UI with all new fields | Complete station finder with details, rating, price, etc. |
| 8 | Both apps working together | Two Android apps sharing one backend |
