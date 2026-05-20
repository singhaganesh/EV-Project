# Station Finder — Detailed Phase-by-Phase Implementation Plan

> **Goal:** Create a **dedicated Spring Boot backend + separate PostgreSQL database** for the station-finder Android app. Zero changes to the main EV backend or main Android app.
>
> **New project location:** `station-finder-backend/` (at project root)
>
> **Total estimated time:** ~8-10 hours

---

## Table of Contents

- [Phase 0: Setup & Preparation](#phase-0-setup--preparation)
- [Phase 1: Create New Spring Boot Project](#phase-1-create-new-spring-boot-project)
- [Phase 2: Data Models + DTOs + Repositories](#phase-2-data-models--dtos--repositories)
- [Phase 3: StationImportService + DataSeeder](#phase-3-stationimportservice--dataseeder)
- [Phase 4: StationService + StationController](#phase-4-stationservice--stationcontroller)
- [Phase 5: StationSyncJob + Configuration](#phase-5-stationsyncjob--configuration)
- [Phase 6: Backend Testing](#phase-6-backend-testing)
- [Phase 7: Station Finder Android App Updates](#phase-7-station-finder-android-app-updates)
- [Phase 8: Integration Testing](#phase-8-integration-testing)
- [Rollback Plan](#rollback-plan)

---

## Phase 0: Setup & Preparation

**Estimated time:** 15 min

### Step 0.1 — Verify Tools

```bash
# Check Java version (must be 21)
java -version

# Check Maven (or we'll use the wrapper)
mvn --version

# Check PostgreSQL is running
psql --version
```

### Step 0.2 — Create Database

```bash
# Connect to PostgreSQL and create the finder database
psql -U postgres -c "CREATE DATABASE ev_station_finder;"

# Verify it was created
psql -U postgres -c "\l" | grep ev_station_finder
```

### Step 0.3 — Get an OCM API Key

1. Go to https://openchargemap.io/site/register
2. Sign up for a free account
3. Get your API key from the dashboard

### Step 0.4 — Review Station Finder Android App

Familiarize yourself with the current app code at `station-finder/app/src/main/java/com/ganesh/stationfinder/`. We'll be:
- **Replacing** the network layer (OCM API → Backend API)
- **Keeping** the UI structure (map, markers, bottom sheet)

---

## Phase 1: Create New Spring Boot Project

**Estimated time:** 30 min

### Step 1.1 — Create Project Directory Structure

```bash
# From project root
mkdir -p station-finder-backend/src/main/java/com/ganesh/finder/{config,controller,model,dto,repository,service}
mkdir -p station-finder-backend/src/main/resources
mkdir -p station-finder-backend/src/test/java/com/ganesh/finder
```

### Step 1.2 — Create pom.xml

**File:** `station-finder-backend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.ganesh</groupId>
    <artifactId>ev-station-finder</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>EV Station Finder Backend</name>
    <description>Dedicated backend for the EV Station Finder Android app</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA + Hibernate -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 1.3 — Create Main Application Class

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/FinderApplication.java`

```java
package com.ganesh.finder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinderApplication.class, args);
    }
}
```

### Step 1.4 — Create application.properties

**File:** `station-finder-backend/src/main/resources/application.properties`

```properties
spring.application.name=ev-station-finder

# ========================================
# PostgreSQL (separate database)
# ========================================
spring.datasource.url=jdbc:postgresql://localhost:5432/ev_station_finder
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ========================================
# Server (different port from main backend)
# ========================================
server.port=8081

# ========================================
# OCM (OpenChargeMap) Import Configuration
# ========================================
ocm.api.key=${OCM_API_KEY:demo_key}
ocm.sync.enabled=true
ocm.sync.radius-km=500
ocm.sync.country-id=101
ocm.sync.interval-cron=0 0 3 * * ?
```

### Step 1.5 — Create API Response Wrapper

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/dto/ApiResponse.java`

```java
package com.ganesh.finder.dto;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

### ✅ Phase 1 Verification

```bash
cd station-finder-backend
# Generate Maven wrapper
mvn -N wrapper:wrapper -Dmaven=3.9.6

# Compile
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 2: Data Models + DTOs + Repositories

**Estimated time:** 45 min

### Step 2.1 — Create Station Entity

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/model/Station.java`

```java
package com.ganesh.finder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 500)
    private String address;

    private String operatingHours;

    private Double pricePerKwh;

    private Double rating;

    private Boolean isOpen;

    // OCM identifiers for dedup
    private Long ocmId;
    private String ocmUuid;

    // Extra metadata stored as JSON string
    @Column(columnDefinition = "TEXT")
    private String meta;

    private LocalDateTime lastSynced;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Station() {}

    public Station(String name, Double latitude, Double longitude, String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public Double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(Double pricePerKwh) { this.pricePerKwh = pricePerKwh; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }

    public Long getOcmId() { return ocmId; }
    public void setOcmId(Long ocmId) { this.ocmId = ocmId; }

    public String getOcmUuid() { return ocmUuid; }
    public void setOcmUuid(String ocmUuid) { this.ocmUuid = ocmUuid; }

    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    public LocalDateTime getLastSynced() { return lastSynced; }
    public void setLastSynced(LocalDateTime lastSynced) { this.lastSynced = lastSynced; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### Step 2.2 — Create ChargerSlot Entity

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/model/ChargerSlot.java`

```java
package com.ganesh.finder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "charger_slots")
public class ChargerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    private String slotLabel;

    private String connectorType;

    private Double powerKw;

    private Boolean isAvailable;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ChargerSlot() {}

    public ChargerSlot(Station station, String slotLabel, String connectorType, Double powerKw) {
        this.station = station;
        this.slotLabel = slotLabel;
        this.connectorType = connectorType;
        this.powerKw = powerKw;
        this.isAvailable = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }

    public String getSlotLabel() { return slotLabel; }
    public void setSlotLabel(String slotLabel) { this.slotLabel = slotLabel; }

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    public Double getPowerKw() { return powerKw; }
    public void setPowerKw(Double powerKw) { this.powerKw = powerKw; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### Step 2.3 — Create DTOs

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/dto/StationMarker.java`

```java
package com.ganesh.finder.dto;

/**
 * Lightweight DTO for map pins.
 */
public class StationMarker {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Boolean available;

    public StationMarker() {}

    public StationMarker(Long id, String name, Double latitude, Double longitude, Boolean available) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
```

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/dto/StationWithScore.java`

```java
package com.ganesh.finder.dto;

import java.util.List;

/**
 * Full station data with scoring and slot info.
 */
public class StationWithScore {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String operatingHours;
    private Double pricePerKwh;
    private Double rating;
    private Boolean isOpen;
    private String meta;

    // Computed fields
    private Double distance;
    private Integer availableSlots;
    private Integer totalSlots;
    private List<String> connectorTypes;
    private List<SlotInfo> slots;

    public static class SlotInfo {
        private Long id;
        private String label;
        private String connectorType;
        private Double powerKw;
        private Boolean isAvailable;

        public SlotInfo() {}

        public SlotInfo(Long id, String label, String connectorType, Double powerKw, Boolean isAvailable) {
            this.id = id;
            this.label = label;
            this.connectorType = connectorType;
            this.powerKw = powerKw;
            this.isAvailable = isAvailable;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getConnectorType() { return connectorType; }
        public void setConnectorType(String connectorType) { this.connectorType = connectorType; }
        public Double getPowerKw() { return powerKw; }
        public void setPowerKw(Double powerKw) { this.powerKw = powerKw; }
        public Boolean getIsAvailable() { return isAvailable; }
        public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    public Double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(Double pricePerKwh) { this.pricePerKwh = pricePerKwh; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }
    public Integer getTotalSlots() { return totalSlots; }
    public void setTotalSlots(Integer totalSlots) { this.totalSlots = totalSlots; }
    public List<String> getConnectorTypes() { return connectorTypes; }
    public void setConnectorTypes(List<String> connectorTypes) { this.connectorTypes = connectorTypes; }
    public List<SlotInfo> getSlots() { return slots; }
    public void setSlots(List<SlotInfo> slots) { this.slots = slots; }
}
```

### Step 2.4 — Create Repositories

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/repository/StationRepository.java`

```java
package com.ganesh.finder.repository;

import com.ganesh.finder.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Find stations within a bounding box (viewport)
    @Query("SELECT s FROM Station s WHERE s.latitude BETWEEN :swLat AND :neLat AND s.longitude BETWEEN :swLng AND :neLng")
    List<Station> findStationsInViewport(
        @Param("swLat") double swLat,
        @Param("neLat") double neLat,
        @Param("swLng") double swLng,
        @Param("neLng") double neLng
    );

    // Search by name or address (case-insensitive)
    @Query("SELECT s FROM Station s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Station> searchByNameOrAddress(@Param("query") String query);

    // Check if station already imported by OCM ID
    Optional<Station> findByOcmId(Long ocmId);

    // Count imported stations
    long count();
}
```

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/repository/ChargerSlotRepository.java`

```java
package com.ganesh.finder.repository;

import com.ganesh.finder.model.ChargerSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerSlotRepository extends JpaRepository<ChargerSlot, Long> {

    List<ChargerSlot> findByStationId(Long stationId);

    List<ChargerSlot> findByStationIdAndIsAvailableTrue(Long stationId);

    long countByStationIdAndIsAvailableTrue(Long stationId);

    long countByStationId(Long stationId);

    void deleteByStationId(Long stationId);
}
```

### ✅ Phase 2 Verification

```bash
cd station-finder-backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 3: StationImportService + DataSeeder

**Estimated time:** 1-2 hours

### Step 3.1 — Create StationImportService

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/service/StationImportService.java`

```java
package com.ganesh.finder.service;

import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StationImportService {

    private static final Logger log = LoggerFactory.getLogger(StationImportService.class);

    @Value("${ocm.api.key}")
    private String ocmApiKey;

    @Value("${ocm.sync.radius-km:500}")
    private int defaultRadiusKm;

    @Value("${ocm.sync.country-id:101}")
    private int countryId;

    private final StationRepository stationRepository;
    private final ChargerSlotRepository chargerSlotRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StationImportService(StationRepository stationRepository,
                                ChargerSlotRepository chargerSlotRepository) {
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch stations from OCM within a radius of a center point.
     *
     * @param centerLat Center latitude
     * @param centerLng Center longitude
     * @param radiusKm  Search radius in kilometers
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

            log.info("OCM import complete: {} imported, {} skipped (total {})",
                    imported, skipped, root.size());

        } catch (Exception e) {
            log.error("OCM import failed: {}", e.getMessage(), e);
        }

        return imported;
    }

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

    @Transactional
    public boolean transformAndSave(JsonNode ocmNode) {
        // Extract OCM ID for deduplication
        long ocmId = ocmNode.has("ID") && !ocmNode.get("ID").isNull()
                ? ocmNode.get("ID").asLong()
                : -1;

        if (ocmId == -1) {
            return false; // Skip stations without ID
        }

        // Check if already imported
        Optional<Station> existing = stationRepository.findByOcmId(ocmId);
        if (existing.isPresent()) {
            // Update lastSynced timestamp
            Station station = existing.get();
            station.setLastSynced(LocalDateTime.now());
            stationRepository.save(station);
            return false; // Already exists
        }

        // Extract AddressInfo
        JsonNode addressInfo = ocmNode.get("AddressInfo");
        if (addressInfo == null) {
            return false;
        }

        String name = addressInfo.has("Title") && !addressInfo.get("Title").isNull()
                ? addressInfo.get("Title").asText()
                : "Unknown Station";

        double latitude = addressInfo.has("Latitude") && !addressInfo.get("Latitude").isNull()
                ? addressInfo.get("Latitude").asDouble()
                : 0;

        double longitude = addressInfo.has("Longitude") && !addressInfo.get("Longitude").isNull()
                ? addressInfo.get("Longitude").asDouble()
                : 0;

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
        String address = addressBuilder.length() > 0
                ? addressBuilder.toString()
                : "Address not available";

        // Extract OCM UUID
        String ocmUuid = ocmNode.has("UUID") && !ocmNode.get("UUID").isNull()
                ? ocmNode.get("UUID").asText()
                : null;

        // Build meta JSON
        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append("{");
        metaBuilder.append("\"ocm_id\":").append(ocmId);
        if (ocmUuid != null) {
            metaBuilder.append(",\"ocm_uuid\":\"").append(escapeJson(ocmUuid)).append("\"");
        }

        // Operator info
        if (ocmNode.has("OperatorInfo") && !ocmNode.get("OperatorInfo").isNull()) {
            JsonNode operator = ocmNode.get("OperatorInfo");
            if (operator.has("Title") && !operator.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_operator\":\"")
                        .append(escapeJson(operator.get("Title").asText())).append("\"");
            }
            if (operator.has("WebsiteURL") && !operator.get("WebsiteURL").isNull()) {
                metaBuilder.append(",\"ocm_website\":\"")
                        .append(escapeJson(operator.get("WebsiteURL").asText())).append("\"");
            }
        }

        // Usage type
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            if (usage.has("Title") && !usage.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_usage_type\":\"")
                        .append(escapeJson(usage.get("Title").asText())).append("\"");
            }
        }

        // Comments
        if (ocmNode.has("GeneralComments") && !ocmNode.get("GeneralComments").isNull()) {
            metaBuilder.append(",\"ocm_comments\":\"")
                    .append(escapeJson(ocmNode.get("GeneralComments").asText())).append("\"");
        }

        metaBuilder.append(",\"source\":\"OCM\"");
        metaBuilder.append(",\"last_synced\":\"").append(LocalDateTime.now()).append("\"");
        metaBuilder.append("}");

        // Determine operating hours from usage type
        String operatingHours = "24 Hours";
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            String usageTitle = usage.has("Title") ? usage.get("Title").asText().toLowerCase() : "";
            if (!usageTitle.contains("pay") && !usageTitle.contains("public")) {
                operatingHours = "Check with operator";
            }
        }

        // Create Station entity
        Station station = new Station(name, latitude, longitude, address);
        station.setOperatingHours(operatingHours);
        station.setPricePerKwh(0.0); // Unknown from OCM
        station.setRating(0.0);
        station.setIsOpen(true);
        station.setOcmId(ocmId);
        station.setOcmUuid(ocmUuid);
        station.setMeta(metaBuilder.toString());
        station.setLastSynced(LocalDateTime.now());

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

                String connectorType = mapConnectorType(conn);
                String slotPrefix = connectorType.replaceAll("[^a-zA-Z0-9]", "");

                for (int i = 0; i < quantity; i++) {
                    ChargerSlot slot = new ChargerSlot(
                            station,
                            slotPrefix + " #" + (i + 1),
                            connectorType,
                            powerKw
                    );
                    slotsToCreate.add(slot);
                }
            }
        }

        // If no connections, create 2 default CCS2 slots
        if (slotsToCreate.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                ChargerSlot slot = new ChargerSlot(
                        station,
                        "CCS2 #" + (i + 1),
                        "CCS2",
                        60.0
                );
                slotsToCreate.add(slot);
            }
        }

        chargerSlotRepository.saveAll(slotsToCreate);
        log.info("Imported station: {} ({} slots)", name, slotsToCreate.size());
        return true;
    }

    private String mapConnectorType(JsonNode connection) {
        if (connection.has("ConnectionType") && !connection.get("ConnectionType").isNull()) {
            JsonNode connType = connection.get("ConnectionType");
            String title = connType.has("Title") && !connType.get("Title").isNull()
                    ? connType.get("Title").asText().toLowerCase()
                    : "";

            if (title.contains("ccs") || title.contains("combo")) {
                return "CCS2";
            }
            if (title.contains("type 2") || title.contains("type2") || title.contains("mennekes")) {
                return "Type 2";
            }
            if (title.contains("chademo")) {
                return "CHAdeMO";
            }
            if (title.contains("type 1") || title.contains("type1") || title.contains("j1772")) {
                return "Type 1";
            }
        }

        // Default based on power level
        if (connection.has("PowerKW") && !connection.get("PowerKW").isNull()) {
            double power = connection.get("PowerKW").asDouble();
            return power > 22 ? "CCS2" : "Type 2";
        }

        return "CCS2";
    }

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

### Step 3.2 — Create DataSeeder

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/config/DataSeeder.java`

```java
package com.ganesh.finder.config;

import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StationImportService stationImportService;

    public DataSeeder(StationImportService stationImportService) {
        this.stationImportService = stationImportService;
    }

    @Override
    public void run(String... args) {
        log.info("====================================");
        log.info("🔍 STATION FINDER DATA SEEDER STARTED");
        log.info("====================================");

        try {
            // Import stations for major Indian cities on first run
            // Using radius 50km for each city center

            log.info("🗺️ Importing stations from Mumbai...");
            int mumbaiCount = stationImportService.importFromOCM(19.0760, 72.8777, 50);
            log.info("✅ Imported {} stations from Mumbai region", mumbaiCount);

            log.info("🗺️ Importing stations from Bangalore...");
            int bangaloreCount = stationImportService.importFromOCM(12.9716, 77.5946, 50);
            log.info("✅ Imported {} stations from Bangalore region", bangaloreCount);

            log.info("🗺️ Importing stations from Delhi...");
            int delhiCount = stationImportService.importFromOCM(28.7041, 77.1025, 50);
            log.info("✅ Imported {} stations from Delhi region", delhiCount);

            log.info("🗺️ Importing stations from Hyderabad...");
            int hyderabadCount = stationImportService.importFromOCM(17.3850, 78.4867, 50);
            log.info("✅ Imported {} stations from Hyderabad region", hyderabadCount);

            log.info("🗺️ Importing stations from Chennai...");
            int chennaiCount = stationImportService.importFromOCM(13.0827, 80.2707, 50);
            log.info("✅ Imported {} stations from Chennai region", chennaiCount);

            int total = mumbaiCount + bangaloreCount + delhiCount + hyderabadCount + chennaiCount;
            log.info("====================================");
            log.info("🎉 SEEDING COMPLETE: {} total stations imported", total);
            log.info("====================================");

        } catch (Exception e) {
            log.warn("⚠️ Seeding skipped or partially completed: {}", e.getMessage());
            log.warn("Make sure OCM_API_KEY environment variable is set.");
        }
    }
}
```

### ✅ Phase 3 Verification

```bash
cd station-finder-backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 4: StationService + StationController

**Estimated time:** 1 hour

### Step 4.1 — Create StationService

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/service/StationService.java`

```java
package com.ganesh.finder.service;

import com.ganesh.finder.dto.StationMarker;
import com.ganesh.finder.dto.StationWithScore;
import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;
    private final ChargerSlotRepository chargerSlotRepository;

    public StationService(StationRepository stationRepository,
                          ChargerSlotRepository chargerSlotRepository) {
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
    }

    /**
     * Get lightweight markers for stations in a viewport.
     */
    public List<StationMarker> getStationsInViewport(double neLat, double neLng,
                                                      double swLat, double swLng) {
        List<Station> stations = stationRepository.findStationsInViewport(
                Math.min(swLat, neLat), Math.max(swLat, neLat),
                Math.min(swLng, neLng), Math.max(swLng, neLng));

        return stations.stream().map(station -> {
            long availableSlots = chargerSlotRepository.countByStationIdAndIsAvailableTrue(station.getId());
            return new StationMarker(
                    station.getId(),
                    station.getName(),
                    station.getLatitude(),
                    station.getLongitude(),
                    availableSlots > 0
            );
        }).collect(Collectors.toList());
    }

    /**
     * Get nearby stations ranked by distance.
     */
    public List<StationWithScore> getNearbyStations(double lat, double lng,
                                                     double radiusKm, int limit) {
        // Calculate approximate bounding box
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Station> stations = stationRepository.findStationsInViewport(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta);

        return stations.stream()
                .map(station -> enrichWithScore(station, lat, lng))
                .filter(s -> s.getDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(StationWithScore::getDistance))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed info for a single station.
     */
    public Optional<StationWithScore> getStationDetail(Long id, double userLat, double userLng) {
        return stationRepository.findById(id)
                .map(station -> enrichWithScore(station, userLat, userLng));
    }

    /**
     * Search stations by name or address.
     */
    public List<StationWithScore> searchStations(String query, double lat, double lng, double radiusKm) {
        List<Station> stations = stationRepository.searchByNameOrAddress(query.toLowerCase().trim());

        return stations.stream()
                .map(station -> enrichWithScore(station, lat, lng))
                .filter(s -> s.getDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(StationWithScore::getDistance))
                .collect(Collectors.toList());
    }

    /**
     * Get total station count.
     */
    public long getStationCount() {
        return stationRepository.count();
    }

    /**
     * Enrich a Station entity with scoring and slot data.
     */
    private StationWithScore enrichWithScore(Station station, double userLat, double userLng) {
        StationWithScore dto = new StationWithScore();
        dto.setId(station.getId());
        dto.setName(station.getName());
        dto.setLatitude(station.getLatitude());
        dto.setLongitude(station.getLongitude());
        dto.setAddress(station.getAddress());
        dto.setOperatingHours(station.getOperatingHours());
        dto.setPricePerKwh(station.getPricePerKwh());
        dto.setRating(station.getRating());
        dto.setIsOpen(station.getIsOpen());
        dto.setMeta(station.getMeta());

        // Calculate distance using Haversine formula
        dto.setDistance(calculateDistance(userLat, userLng, station.getLatitude(), station.getLongitude()));

        // Get slot data
        List<ChargerSlot> slots = chargerSlotRepository.findByStationId(station.getId());
        long availableCount = slots.stream().filter(ChargerSlot::getIsAvailable).count();

        dto.setTotalSlots(slots.size());
        dto.setAvailableSlots((int) availableCount);

        // Extract unique connector types
        List<String> connectorTypes = slots.stream()
                .map(ChargerSlot::getConnectorType)
                .distinct()
                .collect(Collectors.toList());
        dto.setConnectorTypes(connectorTypes);

        // Slot details
        List<StationWithScore.SlotInfo> slotInfos = slots.stream()
                .map(slot -> new StationWithScore.SlotInfo(
                        slot.getId(),
                        slot.getSlotLabel(),
                        slot.getConnectorType(),
                        slot.getPowerKw(),
                        slot.getIsAvailable()
                ))
                .collect(Collectors.toList());
        dto.setSlots(slotInfos);

        return dto;
    }

    /**
     * Haversine distance between two lat/lng points in kilometers.
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
```

### Step 4.2 — Create StationController

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/controller/StationController.java`

```java
package com.ganesh.finder.controller;

import com.ganesh.finder.dto.ApiResponse;
import com.ganesh.finder.dto.StationMarker;
import com.ganesh.finder.dto.StationWithScore;
import com.ganesh.finder.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    /**
     * GET /api/stations/nearby?lat=19.0760&lng=72.8777&radius=50&limit=20
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<?>> getNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<StationWithScore> stations = stationService.getNearbyStations(lat, lng, radius, limit);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + stations.size() + " nearby stations", stations));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7
     */
    @GetMapping("/viewport")
    public ResponseEntity<ApiResponse<?>> getStationsInViewport(
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam double swLat,
            @RequestParam double swLng) {
        try {
            List<StationMarker> markers = stationService.getStationsInViewport(neLat, neLng, swLat, swLng);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + markers.size() + " stations in viewport", markers));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/{id}/detail?lat=19.0760&lng=72.8777
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<?>> getStationDetail(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng) {
        try {
            var detail = stationService.getStationDetail(id, lat, lng);
            if (detail.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Station found", detail.get()));
            }
            return ResponseEntity.status(404).body(ApiResponse.error("Station not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/search?q=nexon&lat=19.0760&lng=72.8777&radius=50
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchStations(
            @RequestParam String q,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius) {
        try {
            List<StationWithScore> results = stationService.searchStations(q, lat, lng, radius);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + results.size() + " stations matching \"" + q + "\"", results));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<?>> getStationCount() {
        try {
            long count = stationService.getStationCount();
            return ResponseEntity.ok(ApiResponse.success("OK", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}
```

### Step 4.3 — Create ImportController (Manual Trigger)

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/controller/ImportController.java`

```java
package com.ganesh.finder.controller;

import com.ganesh.finder.dto.ApiResponse;
import com.ganesh.finder.service.StationImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final StationImportService stationImportService;

    public ImportController(StationImportService stationImportService) {
        this.stationImportService = stationImportService;
    }

    /**
     * POST /api/import/trigger?lat=19.0760&lng=72.8777&radius=50
     * Manually trigger an OCM import.
     */
    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<?>> triggerImport(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") int radius) {
        try {
            int count = stationImportService.importFromOCM(lat, lng, radius);
            return ResponseEntity.ok(ApiResponse.success(
                    "Imported " + count + " stations", Map.of("imported", count)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Import failed: " + e.getMessage()));
        }
    }
}
```

### ✅ Phase 4 Verification

```bash
cd station-finder-backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 5: StationSyncJob + Configuration

**Estimated time:** 30 min

### Step 5.1 — Create StationSyncJob

**File:** `station-finder-backend/src/main/java/com/ganesh/finder/config/StationSyncJob.java`

```java
package com.ganesh.finder.config;

import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ocm.sync.enabled", havingValue = "true", matchIfMissing = false)
public class StationSyncJob {

    private static final Logger log = LoggerFactory.getLogger(StationSyncJob.class);

    private final StationImportService stationImportService;

    @Value("${ocm.sync.radius-km:500}")
    private int radiusKm;

    public StationSyncJob(StationImportService stationImportService) {
        this.stationImportService = stationImportService;
    }

    /**
     * Daily sync at 3:00 AM.
     * Syncs stations around 5 major Indian cities.
     */
    @Scheduled(cron = "${ocm.sync.interval-cron:0 0 3 * * ?}")
    public void syncStations() {
        log.info("Starting scheduled OCM station sync...");

        int total = 0;

        // Mumbai
        total += stationImportService.importFromOCM(19.0760, 72.8777, radiusKm);

        // Bangalore
        total += stationImportService.importFromOCM(12.9716, 77.5946, radiusKm);

        // Delhi
        total += stationImportService.importFromOCM(28.7041, 77.1025, radiusKm);

        // Hyderabad
        total += stationImportService.importFromOCM(17.3850, 78.4867, radiusKm);

        // Chennai
        total += stationImportService.importFromOCM(13.0827, 80.2707, radiusKm);

        log.info("Scheduled OCM sync complete. Total new stations: {}", total);
    }
}
```

### Step 5.2 — Configure application.properties

Already done in Phase 1 (Step 1.4). Just ensure these lines are present:

```properties
# OCM (OpenChargeMap) Import Configuration
ocm.api.key=${OCM_API_KEY:demo_key}
ocm.sync.enabled=true
ocm.sync.radius-km=500
ocm.sync.country-id=101
ocm.sync.interval-cron=0 0 3 * * ?
```

### ✅ Phase 5 Verification

```bash
cd station-finder-backend
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

---

## Phase 6: Backend Testing

**Estimated time:** 1 hour

### Step 6.1 — Create Dockerfile (for reference)

**File:** `station-finder-backend/Dockerfile`

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ev-station-finder-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 6.2 — Start the Backend

```bash
cd station-finder-backend

# Set your OCM API key
export OCM_API_KEY="your_ocm_api_key_here"
# Windows CMD: set OCM_API_KEY=your_ocm_api_key_here
# Windows PowerShell: $env:OCM_API_KEY="your_ocm_api_key_here"

# Build and run
./mvnw spring-boot:run
```

Wait for:
```
Started FinderApplication in X.XXX seconds
```

### Step 6.3 — Test All Endpoints

Open a new terminal and run:

```bash
# 1. Test station count (should print how many were seeded)
curl -s "http://localhost:8081/api/stations/count" | json_pp

# 2. Test nearby stations (Mumbai)
curl -s "http://localhost:8081/api/stations/nearby?lat=19.0760&lng=72.8777&radius=50&limit=5" | json_pp

# 3. Test viewport markers
curl -s "http://localhost:8081/api/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7" | json_pp

# 4. Test search
curl -s "http://localhost:8081/api/stations/search?q=nexon&lat=19.0760&lng=72.8777&radius=50" | json_pp

# 5. Test station detail (replace {id} with an actual ID from step 2)
curl -s "http://localhost:8081/api/stations/detail/1?lat=19.0760&lng=72.8777" | json_pp

# 6. Test manual import trigger (optional — may create duplicates)
curl -s -X POST "http://localhost:8081/api/import/trigger?lat=19.0760&lng=72.8777&radius=50" | json_pp
```

### Expected Results

| Test | Expected |
|------|----------|
| `count` | `{"success":true,"data":{"count":50}}` (or whatever was imported) |
| `nearby` | Array of stations with distance, slots, connector types |
| `viewport` | Array of `{id, name, lat, lng, available}` markers |
| `search` | Filtered results matching query |
| `detail` | Single station with full slot details and distance |
| `import` | `{"success":true,"data":{"imported":0}}` (0 because already imported) |

### Troubleshooting

| Problem | Likely Cause | Fix |
|---------|-------------|-----|
| Empty response | OCM API key not set | `export OCM_API_KEY=your_key` |
| Stations not imported | DB not created yet | `psql -U postgres -c "CREATE DATABASE ev_station_finder;"` |
| Port conflict | Main backend on 8081 too | Check port 8081 is free |
| Import returns 0 | Country ID wrong or radius too small | Try `country-id=101` (India), radius=100 |

---

## Phase 7: Station Finder Android App Updates

**Estimated time:** 2-3 hours

### Step 7.1 — Create BackendApi.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/network/BackendApi.kt`

```kotlin
package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.data.model.ApiResponse
import com.ganesh.stationfinder.data.model.StationMarker
import com.ganesh.stationfinder.data.model.StationWithScore
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApi {

    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 50.0,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<StationWithScore>>

    @GET("api/stations/viewport")
    suspend fun getStationsInViewport(
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double
    ): ApiResponse<List<StationMarker>>

    @GET("api/stations/{id}/detail")
    suspend fun getStationDetail(
        @Path("id") id: Long,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): ApiResponse<StationWithScore>

    @GET("api/stations/search")
    suspend fun searchStations(
        @Query("q") query: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 50.0
    ): ApiResponse<List<StationWithScore>>

    @GET("api/stations/count")
    suspend fun getStationCount(): ApiResponse<Map<String, Any>>
}
```

### Step 7.2 — Create BackendClient.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/network/BackendClient.kt`

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
        .baseUrl(BuildConfig.FINDER_BACKEND_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val api: BackendApi = retrofit.create(BackendApi::class.java)
}
```

### Step 7.3 — Create BackendModels.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/model/BackendModels.kt`

```kotlin
package com.ganesh.stationfinder.data.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching the finder backend's ApiResponse.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

/**
 * Lightweight marker for map pins.
 */
data class StationMarker(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("available") val available: Boolean
)

/**
 * Full station data with scoring and slot info.
 */
data class StationWithScore(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String,
    @SerializedName("operatingHours") val operatingHours: String?,
    @SerializedName("pricePerKwh") val pricePerKwh: Double?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("isOpen") val isOpen: Boolean?,
    @SerializedName("meta") val meta: String?,

    // Computed fields
    @SerializedName("distance") val distance: Double?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    @SerializedName("totalSlots") val totalSlots: Int?,
    @SerializedName("connectorTypes") val connectorTypes: List<String>?,
    @SerializedName("slots") val slots: List<SlotInfo>?
)

/**
 * Individual slot info.
 */
data class SlotInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("label") val label: String,
    @SerializedName("connectorType") val connectorType: String,
    @SerializedName("powerKw") val powerKw: Double?,
    @SerializedName("isAvailable") val isAvailable: Boolean
)
```

### Step 7.4 — Update StationRepository.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/data/repository/StationRepository.kt`

Replace the entire content with backend API calls:

```kotlin
package com.ganesh.stationfinder.data.repository

import android.util.Log
import com.ganesh.stationfinder.data.model.StationMarker
import com.ganesh.stationfinder.data.model.StationWithScore
import com.ganesh.stationfinder.data.network.BackendClient

class StationRepository {

    private val api = BackendClient.api

    suspend fun getNearbyStations(lat: Double, lng: Double, distance: Double = 20.0): List<StationWithScore> {
        Log.d("Repository", "Fetching stations near Lat: $lat, Lng: $lng (Radius: ${distance}km)")
        return try {
            val response = api.getNearbyStations(lat, lng, distance)
            if (response.success && response.data != null) {
                Log.d("Repository", "Backend returned ${response.data.size} stations")
                response.data
            } else {
                Log.w("Repository", "Backend error: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching stations", e)
            emptyList()
        }
    }

    suspend fun getStationsInViewport(
        neLat: Double, neLng: Double,
        swLat: Double, swLng: Double
    ): List<StationMarker> {
        return try {
            val response = api.getStationsInViewport(neLat, neLng, swLat, swLng)
            if (response.success && response.data != null) response.data else emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching viewport stations", e)
            emptyList()
        }
    }

    suspend fun getStationDetail(id: Long, lat: Double, lng: Double): StationWithScore? {
        return try {
            val response = api.getStationDetail(id, lat, lng)
            if (response.success && response.data != null) response.data else null
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching station detail", e)
            null
        }
    }

    suspend fun searchStations(query: String, lat: Double, lng: Double, radius: Double = 50.0): List<StationWithScore> {
        return try {
            val response = api.searchStations(query, lat, lng, radius)
            if (response.success && response.data != null) response.data else emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Error searching stations", e)
            emptyList()
        }
    }

    suspend fun getStationCount(): Int {
        return try {
            val response = api.getStationCount()
            if (response.success && response.data != null) {
                (response.data["count"] as? Double)?.toInt() ?: 0
            } else 0
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching station count", e)
            0
        }
    }
}
```

### Step 7.5 — Update StationViewModel.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/StationViewModel.kt`

Change the `StationUiState` generic type from `OCMStation` to `StationWithScore`:

```kotlin
package com.ganesh.stationfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.stationfinder.data.model.StationWithScore
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
    data class Success(val stations: List<StationWithScore>) : StationUiState()
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

### Step 7.6 — Update StationDetailsSheet.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/StationDetailsSheet.kt`

Replace the content to accept `StationWithScore` and display all the new fields:

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
import com.ganesh.stationfinder.data.model.StationWithScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailsSheet(
    station: StationWithScore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Parse operator name from meta JSON
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
            // Row 1: Icon + Name + Rating
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
                        val rating = station.rating ?: 0.0
                        if (rating > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("★", color = Color(0xFFFFB300), fontSize = 14.sp)
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

            // Row 2: Distance + Available Slots + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip(
                    icon = Icons.Default.NearMe,
                    value = station.distance?.let { String.format("%.1f km", it) } ?: "N/A",
                    label = "Distance"
                )
                InfoChip(
                    icon = Icons.Default.EvStation,
                    value = "${station.availableSlots ?: 0}/${station.totalSlots ?: 0}",
                    label = "Available"
                )
                InfoChip(
                    icon = Icons.Default.CurrencyRupee,
                    value = station.pricePerKwh?.let { "₹${String.format("%.1f", it)}/kWh" } ?: "N/A",
                    label = "Price"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 3: Address + Operating Hours
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = station.address, style = MaterialTheme.typography.bodyLarge)
                    station.operatingHours?.let { hours ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, Modifier.size(16.dp),
                                tint = if (station.isOpen == true) Color(0xFF059669) else Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = hours, style = MaterialTheme.typography.bodySmall,
                                color = if (station.isOpen == true) Color(0xFF059669) else Color.Gray)
                            if (station.isOpen == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("● Open", style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 4: Connector Types
            Text("Connectors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (!station.connectorTypes.isNullOrEmpty()) {
                station.connectorTypes.forEach { connType ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = connType, fontWeight = FontWeight.SemiBold)
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

            // Slot details
            if (!station.slots.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Available Slots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                station.slots.forEach { slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = slot.label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (slot.isAvailable) "✅ Available" else "⛔ In Use",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (slot.isAvailable) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate Button
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = Color(0xFF1A2234))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
    }
}
```

### Step 7.7 — Update MainActivity.kt

**File:** `station-finder/app/src/main/java/com/ganesh/stationfinder/MainActivity.kt`

Key changes:
- Import `StationWithScore` instead of `OCMStation`
- Change `selectedStation` type from `OCMStation?` to `StationWithScore?`
- Update marker rendering to use `station` fields from `StationWithScore`
- Pass `station` to `StationDetailsSheet`

```kotlin
package com.ganesh.stationfinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
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
import com.ganesh.stationfinder.data.model.StationWithScore
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
    var selectedStation by remember { mutableStateOf<StationWithScore?>(null) }

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
        when (val state = uiState) {
            is StationUiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is StationUiState.Success -> {
                if (state.stations.isEmpty()) {
                    Toast.makeText(context, "No stations found in this area", Toast.LENGTH_LONG).show()
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
                    val stations = (uiState as StationUiState.Success).stations
                    stations.forEach { station ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(station.latitude, station.longitude)
                            ),
                            title = station.name,
                            snippet = station.distance?.let {
                                String.format("%.1f km", it)
                            } ?: "",
                            onClick = {
                                selectedStation = station
                                true
                            }
                        )
                    }
                }
            }

            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    val center = cameraPositionState.position.target
                    val zoom = cameraPositionState.position.zoom
                    viewModel.fetchNearbyStationsDebounced(center, zoom)
                }
            }

            // Search Button
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching your location...", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Station Details Bottom Sheet
        selectedStation?.let { station ->
            StationDetailsSheet(
                station = station,
                onDismiss = { selectedStation = null }
            )
        }

        // Loading indicator
        if (uiState is StationUiState.Loading && userLocation != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = Color.Blue
            )
        }
    }
}
```

### Step 7.8 — Update build.gradle.kts

**File:** `station-finder/app/build.gradle.kts`

Add `FINDER_BACKEND_URL` build config:

```kotlin
// At the top, add backendUrl property after the existing OCM key
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")?.trim() ?: ""
val ocmApiKey: String = localProperties.getProperty("OCM_API_KEY")?.trim() ?: ""
val backendUrl: String = localProperties.getProperty("FINDER_BACKEND_URL")?.trim() ?: "http://10.0.2.2:8081/"  // <-- ADD

android {
    // ...
    defaultConfig {
        // ...
        buildConfigField("String", "OCM_API_KEY", "\"${ocmApiKey}\"")
        buildConfigField("String", "FINDER_BACKEND_URL", "\"${backendUrl}\"")  // <-- ADD
    }
}
```

### Step 7.9 — Update local.properties

**File:** `station-finder/local.properties`

```properties
MAPS_API_KEY=your_google_maps_key
OCM_API_KEY=your_ocm_key
FINDER_BACKEND_URL=http://10.0.2.2:8081/
```

> **Note:** Use `10.0.2.2` for Android emulator, use your machine's LAN IP for physical device testing.

### ✅ Phase 7 Verification

```bash
cd station-finder
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

## Phase 8: Integration Testing

**Estimated time:** 1 hour

### Step 8.1 — Start Both Backends

Terminal 1 — Main Backend (existing):
```bash
cd backend
./mvnw spring-boot:run
```

Terminal 2 — Finder Backend (new):
```bash
cd station-finder-backend
export OCM_API_KEY="your_ocm_api_key_here"
./mvnw spring-boot:run
```

Wait for both to show:
```
Started XXX in X.XXX seconds
```

### Step 8.2 — Verify Finder Backend Has Data

```bash
curl -s "http://localhost:8081/api/stations/count"
# Should show: {"success":true,"data":{"count":XX}}

curl -s "http://localhost:8081/api/stations/nearby?lat=19.0760&lng=72.8777&radius=50&limit=3" | head -c 500
```

### Step 8.3 — Verify Main Backend Unaffected

```bash
curl -s "http://localhost:8080/api/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7"
# Should still work — completely independent
```

### Step 8.4 — Install and Test Station Finder App

```bash
cd station-finder
./gradlew installDebug
```

### Step 8.5 — Manual Test Checklist

| # | Test Case | Steps | Expected |
|---|-----------|-------|----------|
| 1 | **App launches** | Open the app | Map centered on your location |
| 2 | **Stations appear** | Wait + tap "Search this area" | Station markers on map |
| 3 | **Station detail** | Tap any marker | Bottom sheet with name, rating, distance, slots, price, connectors |
| 4 | **Missing data** | Tap station with N/A fields | Shows gracefully |
| 5 | **Navigate** | Tap "Start Navigation" | Google Maps opens |
| 6 | **Open/Closed badge** | Check operating hours | Shows "● Open" or hours in gray |
| 7 | **Search area** | Pan map, tap search | New markers load |
| 8 | **No stations** | Pan to remote area, search | "No stations found" toast |

### Step 8.6 — Verify Main EV App Still Works

| # | Test | How |
|---|------|-----|
| 1 | Main app launches | Build and install |
| 2 | Login works | Login with OTP |
| 3 | Station map works | Home screen shows stations |
| 4 | No regression | All existing features work as before |

---

## Rollback Plan

### If Finder Backend Has Issues
Since it's a **completely separate project**, rollback is simply:
```bash
# Stop the finder backend
# Delete the project
rm -rf station-finder-backend/

# Drop the database
psql -U postgres -c "DROP DATABASE ev_station_finder;"

# Station-finder app still works with OCM directly (if OCM_API_KEY set)
# Just revert the git changes to station-finder/ folder
cd station-finder
git checkout -- .
```

### If Station Finder App Changes Have Issues
```bash
cd station-finder
git checkout -- .
# Then restore OCM_API_KEY in local.properties
```

### Main Backend & Main App
**Nothing to roll back** — 0 files were changed.

---

## Summary

| Phase | What | Files Created | Files Modified | Est. Time |
|-------|------|---------------|----------------|-----------|
| 1 | New Spring Boot project | 5 (pom, application, ApiResponse, FinderApp, config) | 0 | 30 min |
| 2 | Models + DTOs + Repos | 6 (Station, ChargerSlot, StationMarker, StationWithScore, 2 repos) | 0 | 45 min |
| 3 | OCM Import + DataSeeder | 2 (StationImportService, DataSeeder) | 0 | 1-2 hr |
| 4 | StationService + Controllers | 3 (StationService, StationController, ImportController) | 0 | 1 hr |
| 5 | SyncJob + Config | 1 (StationSyncJob) | 0 | 30 min |
| 6 | Backend testing | 1 (Dockerfile) | 0 | 1 hr |
| 7 | Android app updates | 3 (BackendApi, BackendClient, BackendModels) | 4 (Repository, VM, Sheet, Activity, Gradle) | 2-3 hr |
| 8 | Integration testing | 0 | 0 | 1 hr |

**Total:** ~21 files created, ~5 files modified, **0 files changed in main backend or main app** ✅
