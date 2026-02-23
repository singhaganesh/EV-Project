package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.StationMarkerDTO;
import com.ganesh.EV_Project.dto.StationPinDTO;
import com.ganesh.EV_Project.dto.StationScoreDTO;
import com.ganesh.EV_Project.dto.ViewportResponseDTO;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.enums.SlotStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StationRecommendationService {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerSlotRepository chargerSlotRepository;

    @Autowired
    private com.ganesh.EV_Project.repository.IoTSensorDataRepository ioTSensorDataRepository;

    // Weights from paper (approximate or configurable)
    private static final double W1 = 0.35; // Traffic
    private static final double W2 = 0.30; // Grid
    private static final double W3 = 0.20; // Parking
    private static final double W4 = 0.15; // Accessibility

    // ===== VIEWPORT: Lightweight markers for map pins =====
    public List<StationMarkerDTO> getStationsInViewport(double swLat, double neLat, double swLng, double neLng) {
        List<Station> stations = stationRepository.findByBoundingBox(swLat, neLat, swLng, neLng);

        return stations.stream().map(station -> {
            List<ChargerSlot> slots = chargerSlotRepository.findByStation(station);
            boolean available = slots.stream().anyMatch(s -> s.getStatus() == SlotStatus.AVAILABLE);

            return StationMarkerDTO.builder()
                    .id(station.getId())
                    .name(station.getName())
                    .latitude(station.getLatitude())
                    .longitude(station.getLongitude())
                    .available(available)
                    .build();
        }).collect(Collectors.toList());
    }

    // ===== NEARBY: Top N stations with full scoring for bottom pager =====
    public List<StationScoreDTO> getNearbyStationsRanked(double userLat, double userLng, double radiusKm, int limit) {
        List<Station> allStations = stationRepository.findAll();
        List<StationScoreDTO> rankedStations = new ArrayList<>();

        for (Station station : allStations) {
            double distance = calculateHaversineDistance(userLat, userLng, station.getLatitude(),
                    station.getLongitude());

            // Filter by radius to avoid scoring distant stations
            if (distance > radiusKm)
                continue;

            rankedStations.add(buildStationScoreDTO(station, distance));
        }

        // Sort by Distance Ascending (Closest first)
        rankedStations.sort(Comparator.comparingDouble(StationScoreDTO::getDistance));

        // Return top N
        return rankedStations.stream().limit(limit).collect(Collectors.toList());
    }

    // ===== Overload for backward compatibility (returns all within radius) =====
    public List<StationScoreDTO> getNearbyStationsRanked(double userLat, double userLng, double radiusKm) {
        return getNearbyStationsRanked(userLat, userLng, radiusKm, Integer.MAX_VALUE);
    }

    // ===== DETAIL: Full score for a single station (on marker click) =====
    public StationScoreDTO getStationDetail(Long stationId, double userLat, double userLng) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));

        double distance = calculateHaversineDistance(userLat, userLng, station.getLatitude(),
                station.getLongitude());

        return buildStationScoreDTO(station, distance);
    }

    // ===== VIEWPORT + NEARBY: Unified endpoint (Optimized) =====
    public ViewportResponseDTO getViewportWithNearby(
            double swLat, double neLat, double swLng, double neLng,
            double userLat, double userLng, int limit) {

        // 1. Fetch lightweight pins only (No N+1 query, no Slot loading)
        List<StationPinDTO> allPins = stationRepository.findPinsByBoundingBox(swLat, neLat, swLng, neLng);

        // 2. Calculate distance and sort (in-memory, fast)
        // We use a temporary wrapper to hold pin + distance
        record PinWithDistance(StationPinDTO pin, double distance) {
        }

        List<PinWithDistance> sortedPins = allPins.stream()
                .map(pin -> new PinWithDistance(pin, calculateHaversineDistance(
                        userLat, userLng, pin.getLatitude(), pin.getLongitude())))
                .sorted(Comparator.comparingDouble(PinWithDistance::distance))
                .collect(Collectors.toList());

        // 3. Split: Top N (needs full data) vs Rest (pins only)
        List<PinWithDistance> topNPins = sortedPins.stream().limit(limit).collect(Collectors.toList());

        java.util.Set<Long> topNIds = topNPins.stream()
                .map(p -> p.pin().getId())
                .collect(Collectors.toSet());

        List<StationPinDTO> otherPins = allPins.stream()
                .filter(pin -> !topNIds.contains(pin.getId()))
                .collect(Collectors.toList());

        // 4. Fetch FULL data ONLY for the Top N nearest stations
        List<StationScoreDTO> nearbyStations = new ArrayList<>();
        if (!topNIds.isEmpty()) {
            List<Station> topNStations = stationRepository.findAllById(topNIds);

            // Re-map to maintain the sorted distance order
            for (PinWithDistance pwd : topNPins) {
                topNStations.stream()
                        .filter(s -> s.getId().equals(pwd.pin().getId()))
                        .findFirst()
                        .ifPresent(station -> {
                            nearbyStations.add(buildStationScoreDTO(station, pwd.distance()));
                        });
            }
        }

        return ViewportResponseDTO.builder()
                .nearbyStations(nearbyStations)
                .otherPins(otherPins)
                .build();
    }

    // ===== Shared DTO builder =====
    private StationScoreDTO buildStationScoreDTO(Station station, double distance) {
        List<ChargerSlot> slots = chargerSlotRepository.findByStation(station);
        int totalSlots = slots.size();
        int availableSlots = (int) slots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();

        double trafficScore = generateStableRandomScore(station.getId(), "TRAFFIC");
        double gridScore = generateStableRandomScore(station.getId(), "GRID");
        double parkingScore = generateStableRandomScore(station.getId(), "PARKING");
        double accessScore = generateStableRandomScore(station.getId(), "ACCESS");

        double finalScore = (trafficScore * W1) + (gridScore * W2) + (parkingScore * W3) + (accessScore * W4);

        List<String> connectorTypes = slots.stream()
                .map(slot -> slot.getConnectorType().toString())
                .distinct()
                .collect(Collectors.toList());

        String lastActive = getLastActiveTime(station);
        double rating = station.getRating() != null ? station.getRating() : 0.0;

        return new StationScoreDTO(
                station, distance, finalScore,
                trafficScore, gridScore, parkingScore, accessScore,
                availableSlots, totalSlots,
                connectorTypes, lastActive, rating);
    }

    // Equation 2: Haversine Formula
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double generateStableRandomScore(Long id, String seed) {
        Random random = new Random(id.hashCode() + seed.hashCode());
        return 0.5 + (0.49 * random.nextDouble());
    }

    private String getLastActiveTime(Station station) {
        if (station.getLastUsedTime() == null) {
            return "Never";
        }

        java.time.LocalDateTime lastUsed = station.getLastUsedTime();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(lastUsed, now);

        long seconds = duration.getSeconds();
        if (seconds < 60)
            return "Just now";

        long minutes = duration.toMinutes();
        if (minutes < 60)
            return minutes + " min ago";

        long hours = duration.toHours();
        if (hours < 24)
            return hours + " hr ago";

        long days = duration.toDays();
        if (days < 30)
            return days + " days ago";

        long months = days / 30;
        if (months < 12)
            return months + " months ago";

        long years = days / 365;
        return years + " years ago";
    }
}
