# Implementation Plan: Hour-Aligned Metrics Trend Indication

This document outlines the step-by-step instructions for implementing **Strategy A: Hour-Aligned Comparison** for the Pump Owner Dashboard. 

This strategy compares today's performance up to the current hour/minute against yesterday's performance up to the exact same hour/minute (e.g. today 12:00 AM - 3:00 PM vs yesterday 12:00 AM - 3:00 PM). This prevents false negative trends early in the day and provides an accurate, real-time indicator.

---

## 1. Backend: Repository Updates

Modify the `ChargingSessionRepository` interface to add queries that calculate totals between specific timestamps (instead of just "since start of today").

### File to Modify:
* `backend/src/main/java/com/ganesh/EV_Project/repository/ChargingSessionRepository.java`

### Code to Add:
```java
    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.energyKwh) FROM ChargingSession s " +
            "WHERE s.booking.slot.station.owner.id = :ownerId " +
            "AND s.endTime >= :start AND s.endTime <= :end AND s.status = 'COMPLETED'")
    Double sumEnergyByOwnerBetween(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s " +
            "WHERE s.booking.slot.station.owner.id = :ownerId " +
            "AND s.endTime >= :start AND s.endTime <= :end AND s.paymentStatus = 'PAID'")
    Double sumEarningsByOwnerBetween(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
```

---

## 2. Backend: DTO Updates

Add two new fields to carry the trend percentages back to the frontend dashboard.

### File to Modify:
* `backend/src/main/java/com/ganesh/EV_Project/dto/OwnerStationStatsDTO.java`

### Code to Modify:
```java
package com.ganesh.EV_Project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerStationStatsDTO {
    private Long totalStations;
    private Long activeStationsCount;
    private Long activeChargers;
    private Long inUseChargers;
    private Double utilizationRate;
    private Double todayEnergyKwh;
    private Double todayEarnings;
    
    // Add these trend fields:
    private Double energyTrendPercentage;
    private Double earningsTrendPercentage;
}
```

---

## 3. Backend: Service Updates

Update the stats service logic to define the current time boundary and yesterday's matching boundary, fetch both sets of sums, calculate the trend percentages, and build the DTO.

### File to Modify:
* `backend/src/main/java/com/ganesh/EV_Project/service/StationService.java`

### Code to Modify (inside `getOwnerRealtimeStats` method):

Replace the existing `// ── CALCULATE TODAY'S METRICS ──` section:

```java
        // ── CALCULATE TODAY'S METRICS & HOUR-ALIGNED TRENDS ──
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
        
        java.time.LocalDateTime startOfYesterday = java.time.LocalDate.now().minusDays(1).atStartOfDay();
        java.time.LocalDateTime sameTimeYesterday = now.minusDays(1);

        // Fetch Today's metrics (Start of today to Current Time)
        Double todayEnergy = chargingSessionRepository.sumEnergyByOwnerBetween(ownerId, startOfToday, now);
        Double todayEarnings = chargingSessionRepository.sumEarningsByOwnerBetween(ownerId, startOfToday, now);
        todayEnergy = (todayEnergy != null) ? todayEnergy : 0.0;
        todayEarnings = (todayEarnings != null) ? todayEarnings : 0.0;

        // Fetch Yesterday's Hour-Aligned metrics (Start of yesterday to Yesterday's Current Time)
        Double yesterdayEnergy = chargingSessionRepository.sumEnergyByOwnerBetween(ownerId, startOfYesterday, sameTimeYesterday);
        Double yesterdayEarnings = chargingSessionRepository.sumEarningsByOwnerBetween(ownerId, startOfYesterday, sameTimeYesterday);
        yesterdayEnergy = (yesterdayEnergy != null) ? yesterdayEnergy : 0.0;
        yesterdayEarnings = (yesterdayEarnings != null) ? yesterdayEarnings : 0.0;

        // Calculate Trends (safe from division by zero)
        double energyTrend = calculateTrendPercentage(todayEnergy, yesterdayEnergy);
        double earningsTrend = calculateTrendPercentage(todayEarnings, yesterdayEarnings);

        return OwnerStationStatsDTO.builder()
                .totalStations(totalStations)
                .activeStationsCount(activeStationsCount)
                .activeChargers(activeChargers)
                .inUseChargers(inUseChargers)
                .utilizationRate(roundedUtilizationRate)
                .todayEnergyKwh(Math.round(todayEnergy * 10.0) / 10.0)
                .todayEarnings(Math.round(todayEarnings * 100.0) / 100.0)
                .energyTrendPercentage(energyTrend)
                .earningsTrendPercentage(earningsTrend)
                .build();
```

Add this helper method inside the class:
```java
    private double calculateTrendPercentage(double current, double previous) {
        if (previous == 0.0) {
            return current > 0.0 ? 100.0 : 0.0;
        }
        double diff = current - previous;
        double percentage = (diff / previous) * 100.0;
        return Math.round(percentage * 10.0) / 10.0; // Rounds to 1 decimal place (e.g. 15.4)
    }
```

---

## 4. Frontend: React State & UI Updates

Modify the Vite/React dashboard page to read these trend values and dynamically style the trend badges.

### File to Modify:
* `web/src/pages/owner/PumpOwnerDashboard.jsx`

### Code to Modify:

1. Update the state structure:
```javascript
    const [stats, setStats] = useState({
        totalStations: 0,
        activeStationsCount: 0,
        todayEnergyKwh: 0,
        todayEarnings: 0,
        utilizationRate: 0,
        energyTrendPercentage: 0,   // Add this
        earningsTrendPercentage: 0   // Add this
    });
```

2. Update the state setting logic inside `fetchDashboardData`:
```javascript
            const statsData = statsRes.data?.data || statsRes.data || {};
            setStats({
                totalStations: statsData.totalStations || 0,
                activeStationsCount: statsData.activeStationsCount || 0,
                todayEnergyKwh: statsData.todayEnergyKwh || 0,
                todayEarnings: statsData.todayEarnings || 0,
                utilizationRate: statsData.utilizationRate || 0,
                energyTrendPercentage: statsData.energyTrendPercentage || 0, // Add this
                earningsTrendPercentage: statsData.earningsTrendPercentage || 0 // Add this
            });
```

3. Update the `StatCard` tags to map dynamic attributes:
```jsx
                <StatCard
                    title="Energy Dispensed (Today)"
                    value={`${stats.todayEnergyKwh.toFixed(1)} kWh`}
                    icon={Zap}
                    iconColor="bg-cyan-500"
                    trend={stats.energyTrendPercentage >= 0 ? 'up' : 'down'}
                    trendValue={`${stats.energyTrendPercentage >= 0 ? '+' : ''}${stats.energyTrendPercentage.toFixed(1)}%`}
                    trendLabel="vs yesterday (same time)"
                />
                <StatCard
                    title="Today's Earnings"
                    value={`₹${stats.todayEarnings.toLocaleString('en-IN')}`}
                    icon={Wallet}
                    iconColor="bg-emerald-500"
                    trend={stats.earningsTrendPercentage >= 0 ? 'up' : 'down'}
                    trendValue={`${stats.earningsTrendPercentage >= 0 ? '+' : ''}${stats.earningsTrendPercentage.toFixed(1)}%`}
                    trendLabel="vs yesterday (same time)"
                />
```
