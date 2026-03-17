package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.BookingRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChargingSimulatorService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BookingRepository bookingRepository;

    // Track active sessions: <BookingId, SimulatedSession>
    private final Map<Long, SimulatedSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Starts the smart simulation for a new charging session.
     */
    public void startSimulation(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            SimulatedSession session = createInitialSession(booking);
            activeSessions.put(bookingId, session);
            log.info("Smart Simulation started for Booking {}: Vehicle {}, Max Power {}kW", 
                bookingId, booking.getVehicleType(), session.maxPowerKw);
        });
    }

    /**
     * Halts the simulation and removes it from the active registry.
     */
    public SimulatedSession stopSimulation(Long bookingId) {
        log.info("Smart Simulation halted for Booking {}", bookingId);
        return activeSessions.remove(bookingId);
    }

    /**
     * Physics Engine: Runs every 5 seconds to update all active sessions.
     */
    @Scheduled(fixedRate = 5000)
    public void runPhysicsTick() {
        if (activeSessions.isEmpty()) return;

        // Group by station for Load Balancing simulation
        Map<Long, Integer> stationChargingCount = new ConcurrentHashMap<>();
        activeSessions.values().forEach(s -> 
            stationChargingCount.merge(s.stationId, 1, Integer::sum));

        activeSessions.forEach((bookingId, session) -> {
            updateSessionVitals(session, stationChargingCount.get(session.stationId));
            broadcastTelemetry(session);
        });
    }

    private void updateSessionVitals(SimulatedSession session, int carsAtStation) {
        // 1. Load Balancing Logic: Split power if multiple cars are charging
        double availablePower = session.maxPowerKw / Math.max(1, carsAtStation);
        
        // 2. SoC Tapering Logic: Slow down after 80% to protect battery
        if (session.socPercentage >= 80.0) {
            double taperFactor = 1.0 - ((session.socPercentage - 80.0) / 20.0); // Drops to 0 at 100%
            availablePower *= Math.max(0.1, taperFactor);
        }

        // 3. V-I Relationship Math
        // Power (kW) = (Voltage * Current) / 1000
        session.voltageV = 380.0 + ThreadLocalRandom.current().nextDouble(-2.0, 5.0); // Slight grid noise
        session.currentA = (availablePower * 1000.0) / session.voltageV;
        session.powerKw = (session.voltageV * session.currentA) / 1000.0;

        // 4. Energy Accumulation (kWh)
        double addedEnergy = (session.powerKw * 5.0) / 3600.0;
        session.energyDispensedKwh += addedEnergy;
        session.socPercentage = Math.min(100.0, session.socPercentage + (addedEnergy / session.batteryCapacityKwh * 100.0));

        // 5. Thermal Simulation
        // Temperature rises based on Current^2 (Joule Heating)
        double targetTemp = 25.0 + (Math.pow(session.currentA / 100.0, 2) * 15.0);
        if (session.connectorTempC < targetTemp) {
            session.connectorTempC += 0.5;
        } else {
            session.connectorTempC -= 0.2;
        }

        // 6. Cost & ETC
        session.totalCost = session.energyDispensedKwh * session.pricePerKwh;
        if (session.powerKw > 0) {
            session.minutesRemaining = ((session.batteryCapacityKwh * (1.0 - (session.socPercentage / 100.0))) / session.powerKw) * 60.0;
        } else {
            session.minutesRemaining = 0;
        }
    }

    private void broadcastTelemetry(SimulatedSession session) {
        // Topic 1: Private (Full detail for the charging user)
        messagingTemplate.convertAndSend("/topic/session/" + session.bookingId, session);

        // Topic 2: Public (Aggregated for station searchers)
        Map<String, Object> publicUpdate = Map.of(
            "slotId", session.slotId,
            "status", "CHARGING",
            "soc", Math.round(session.socPercentage),
            "timeLeft", (int) Math.ceil(session.minutesRemaining)
        );
        messagingTemplate.convertAndSend("/topic/station/" + session.stationId, publicUpdate);

        // Topic 3: Owner (Health metrics for the dashboard)
        Map<String, Object> healthUpdate = Map.of(
            "slotId", session.slotId,
            "temp", String.format("%.1f°C", session.connectorTempC),
            "voltage", Math.round(session.voltageV),
            "current", Math.round(session.currentA),
            "status", session.connectorTempC > 85 ? "CRITICAL_HEAT" : "OPERATIONAL"
        );
        messagingTemplate.convertAndSend("/topic/owner/station/" + session.stationId, healthUpdate);
    }

    private SimulatedSession createInitialSession(Booking booking) {
        ChargerSlot slot = booking.getSlot();
        Station station = slot.getDispensary() != null ? slot.getDispensary().getStation() : slot.getStation();
        boolean isTruck = booking.getVehicleType() == com.ganesh.EV_Project.enums.VehicleType.TRUCK;

        // Ensure we have a valid max power, fallback to 22kW if null
        double maxPower = slot.getPowerKw() != null ? slot.getPowerKw() : 22.0;
        
        // Ensure we have a valid price, fallback to 15.0 if null
        double price = 15.0;
        if (station != null) {
            Double p = isTruck ? station.getTruckPricePerKwh() : station.getPricePerKwh();
            if (p != null) price = p;
        }

        return SimulatedSession.builder()
            .bookingId(booking.getId())
            .slotId(slot.getId())
            .stationId(station != null ? station.getId() : 0L)
            .maxPowerKw(maxPower)
            .batteryCapacityKwh(isTruck ? 250.0 : 65.0) 
            .socPercentage(ThreadLocalRandom.current().nextDouble(10.0, 40.0)) 
            .pricePerKwh(price)
            .connectorTempC(28.0) 
            .energyDispensedKwh(0.0)
            .totalCost(0.0)
            .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class SimulatedSession {
        private Long bookingId;
        private Long slotId;
        private Long stationId;
        
        // Live Metrics
        private double powerKw;
        private double energyDispensedKwh;
        private double socPercentage;
        private double voltageV;
        private double currentA;
        private double connectorTempC;
        private double totalCost;
        private double minutesRemaining;
        
        // Static Config
        private double maxPowerKw;
        private double batteryCapacityKwh;
        private double pricePerKwh;
    }
}
