package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.OwnerStationStatsDTO;
import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerSlotRepository chargerSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.ganesh.EV_Project.repository.ChargingSessionRepository chargingSessionRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Station> getStationsByOwnerId(Long ownerId) {
        return stationRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public OwnerStationStatsDTO getOwnerRealtimeStats(Long ownerId) {
        long totalStations = stationRepository.countByOwnerId(ownerId);
        long activeStationsCount = stationRepository.countActiveByOwnerId(ownerId);
        long activeChargers = chargerSlotRepository.countByOwnerIdAndStatusNot(ownerId, SlotStatus.MAINTENANCE);

        List<SlotStatus> inUseStatuses = List.of(SlotStatus.RESERVED, SlotStatus.BOOKED, SlotStatus.CHARGING);
        long inUseChargers = chargerSlotRepository.countByOwnerIdAndStatusIn(ownerId, inUseStatuses);

        if (inUseChargers > activeChargers) {
            inUseChargers = activeChargers;
        }

        double utilizationRate = activeChargers == 0
                ? 0.0
                : (inUseChargers * 100.0) / activeChargers;

        double roundedUtilizationRate = Math.round(utilizationRate * 10.0) / 10.0;

        // ── CALCULATE TODAY'S METRICS & HOUR-ALIGNED TRENDS ──
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();

        java.time.LocalDateTime startOfYesterday = java.time.LocalDate.now().minusDays(1).atStartOfDay();
        java.time.LocalDateTime sameTimeYesterday = now.minusDays(1);

        // Today: start of today → now
        Double todayEnergy = chargingSessionRepository.sumEnergyByOwnerBetween(ownerId, startOfToday, now);
        Double todayEarnings = chargingSessionRepository.sumEarningsByOwnerBetween(ownerId, startOfToday, now);
        todayEnergy = (todayEnergy != null) ? todayEnergy : 0.0;
        todayEarnings = (todayEarnings != null) ? todayEarnings : 0.0;

        // Yesterday, hour-aligned: start of yesterday → same time yesterday
        Double yesterdayEnergy = chargingSessionRepository.sumEnergyByOwnerBetween(ownerId, startOfYesterday, sameTimeYesterday);
        Double yesterdayEarnings = chargingSessionRepository.sumEarningsByOwnerBetween(ownerId, startOfYesterday, sameTimeYesterday);
        yesterdayEnergy = (yesterdayEnergy != null) ? yesterdayEnergy : 0.0;
        yesterdayEarnings = (yesterdayEarnings != null) ? yesterdayEarnings : 0.0;

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
    }

    // Percentage change of current vs previous, safe against division by zero.
    private double calculateTrendPercentage(double current, double previous) {
        if (previous == 0.0) {
            return current > 0.0 ? 100.0 : 0.0;
        }
        double percentage = ((current - previous) / previous) * 100.0;
        return Math.round(percentage * 10.0) / 10.0;
    }

    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new APIException("Station not found"));
    }

    @Transactional
    public Station addStation(Station station) {
        System.out.println("Adding station: " + station.getName());
        if (station.getDispensaries() != null) {
            System.out.println("Contains " + station.getDispensaries().size() + " dispensaries");
            for (Dispensary d : station.getDispensaries()) {
                d.setStation(station);
                // Ensure connectorType has a value before persisting 
                if (d.getConnectorType() == null) {
                    d.setConnectorType(ConnectorType.CCS2);
                }
            }
        }

        Station savedStation = stationRepository.save(station);

        if (savedStation.getDispensaries() != null) {
            for (Dispensary d : savedStation.getDispensaries()) {
                int gunCount = d.getNumberOfGuns() != null ? d.getNumberOfGuns() : 2;
                double powerPerGun = d.getTotalPowerKw() != null ? d.getTotalPowerKw() / gunCount : 30.0;
                ConnectorType ct = d.getConnectorType() != null ? d.getConnectorType() : ConnectorType.CCS2;
                // Derive SlotType from connector: CCS2 → DC, TYPE_2 → AC
                SlotType slotType = (ct == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;

                for (int i = 1; i <= gunCount; i++) {
                    com.ganesh.EV_Project.model.ChargerSlot slot = com.ganesh.EV_Project.model.ChargerSlot.builder()
                            .station(savedStation)
                            .dispensary(d)
                            .slotLabel(d.getName() + " - Gun " + i)
                            .slotType(slotType)
                            .status(SlotStatus.AVAILABLE)
                            .connectorType(ct)
                            .powerKw(powerPerGun)
                            .build();
                    chargerSlotRepository.save(slot);
                }
            }
        }

        return savedStation;
    }

    public Station updateStation(Long id, Station updatedStation) {
        return stationRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedStation.getName());
                    existing.setLatitude(updatedStation.getLatitude());
                    existing.setLongitude(updatedStation.getLongitude());
                    existing.setAddress(updatedStation.getAddress());
                    existing.setMeta(updatedStation.getMeta());
                    existing.setOperatingHours(updatedStation.getOperatingHours());
                    existing.setPricePerKwh(updatedStation.getPricePerKwh());
                    existing.setTruckPricePerKwh(updatedStation.getTruckPricePerKwh());
                    existing.setCostPerKwh(updatedStation.getCostPerKwh());
                    return stationRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Station not found"));
    }

    @Transactional
    public void deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new APIException("Station not found"));

        List<com.ganesh.EV_Project.model.ChargerSlot> slots = chargerSlotRepository.findByStation(station);
        for (com.ganesh.EV_Project.model.ChargerSlot slot : slots) {
            bookingRepository.deleteBySlot(slot);
        }

        chargerSlotRepository.deleteByStation(station);
        stationRepository.delete(station);
    }
}
