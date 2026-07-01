package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChargingSimulatorService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BookingRepository bookingRepository;
    private final ChargingSessionRepository chargingSessionRepository;
    private final ChargingCompletionService completionService;

    // Persist a snapshot of progress roughly every 6 ticks (~30s) so a restart
    // can resume from the last known state instead of orphaning the session.
    private static final int SNAPSHOT_EVERY_TICKS = 6;

    // Hard cap on session length so a forgotten session can't accrue cost forever.
    @org.springframework.beans.factory.annotation.Value("${app.charging.max-session-minutes:240}")
    private long maxSessionMinutes;

    // Wall-clock acceleration for the simulation: each 5s tick delivers this many
    // multiples of real energy, so SoC climbs (and the session completes) faster.
    // 1.0 = realtime (production); dev/demo overrides this to finish in minutes.
    @org.springframework.beans.factory.annotation.Value("${app.charging.simulation-speed:1.0}")
    private double simulationSpeed;

    // Track active sessions: <BookingId, SimulatedSession>
    private final Map<Long, SimulatedSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * On startup, rebuild in-memory simulators for any session left ONGOING by a
     * previous run, resuming from the last persisted SoC/energy/cost.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverActiveSessions() {
        List<ChargingSession> ongoing = chargingSessionRepository.findByStatus("ONGOING");
        for (ChargingSession cs : ongoing) {
            try {
                if (cs.getBooking() == null) continue;
                SimulatedSession session = rebuildSession(cs);
                activeSessions.put(cs.getBooking().getId(), session);
                log.info("Recovered active simulation for Booking {} (SoC {}%)",
                        cs.getBooking().getId(), Math.round(session.socPercentage));
            } catch (Exception e) {
                log.error("Failed to recover session {}: {}", cs.getId(), e.getMessage());
            }
        }
        if (!ongoing.isEmpty()) {
            log.info("Recovered {} active charging session(s) after restart", ongoing.size());
        }
    }

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

            // Battery full / overtime → finalize server-side so the session ends
            // even if the user's app is closed. Only after the DB is actually
            // finalized do we drop the session and broadcast the completed frame —
            // otherwise the app would be told "done" while the session is still
            // ONGOING and would restart charging. On failure we keep it and retry
            // next tick.
            if (session.completedReady) {
                if (autoComplete(session)) {
                    activeSessions.remove(bookingId);
                    broadcastTelemetry(session);
                }
                return;
            }

            broadcastTelemetry(session);
            if (++session.ticksSinceSnapshot >= SNAPSHOT_EVERY_TICKS) {
                session.ticksSinceSnapshot = 0;
                persistSnapshot(session);
            }
        });
    }

    /** Finalizes a full/overtime session; returns true once the DB is finalized. */
    private boolean autoComplete(SimulatedSession session) {
        try {
            var csOpt = chargingSessionRepository.findByBookingId(session.bookingId);
            if (csOpt.isEmpty()) return true; // nothing to finalize — drop it
            completionService.finalizeSession(csOpt.get().getId(),
                    session.energyDispensedKwh, session.totalCost);
            log.info("Auto-completed charging for booking {} at {}% SoC",
                    session.bookingId, Math.round(session.socPercentage));
            return true;
        } catch (Exception e) {
            log.error("Auto-complete failed for booking {}: {}", session.bookingId, e.getMessage());
            return false;
        }
    }

    /** Persist live progress so a restart can resume this session authoritatively. */
    private void persistSnapshot(SimulatedSession session) {
        try {
            chargingSessionRepository.findByBookingId(session.bookingId).ifPresent(cs -> {
                cs.setEnergyKwh(session.energyDispensedKwh);
                cs.setSocPercentage(session.socPercentage);
                cs.setTotalCost(session.totalCost);
                chargingSessionRepository.save(cs);
            });
        } catch (Exception e) {
            log.warn("Failed to snapshot session for booking {}: {}", session.bookingId, e.getMessage());
        }
    }

    /** Rebuilds a simulator session from a persisted (ONGOING) ChargingSession. */
    private SimulatedSession rebuildSession(ChargingSession cs) {
        Booking booking = cs.getBooking();
        ChargerSlot slot = booking.getSlot();
        Station station = slot.getDispensary() != null ? slot.getDispensary().getStation() : slot.getStation();
        boolean isTruck = booking.getVehicleType() == com.ganesh.EV_Project.enums.VehicleType.TRUCK;

        double maxPower = slot.getPowerKw() != null ? slot.getPowerKw() : 22.0;
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
            .socPercentage(cs.getSocPercentage() != null ? cs.getSocPercentage() : 20.0)
            .pricePerKwh(price)
            .connectorTempC(28.0)
            .energyDispensedKwh(cs.getEnergyKwh() != null ? cs.getEnergyKwh() : 0.0)
            .totalCost(cs.getTotalCost() != null ? cs.getTotalCost() : 0.0)
            .startedAt(cs.getStartTime() != null ? cs.getStartTime() : java.time.LocalDateTime.now())
            .build();
    }

    private void updateSessionVitals(SimulatedSession session, int carsAtStation) {
        // Stop accruing energy/cost once the battery is full or the session has
        // run past the hard duration cap. This bounds cost for forgotten sessions.
        boolean full = session.socPercentage >= 100.0;
        boolean overtime = session.startedAt != null
                && java.time.Duration.between(session.startedAt, java.time.LocalDateTime.now()).toMinutes() >= maxSessionMinutes;
        if (full || overtime) {
            session.powerKw = 0.0;
            session.currentA = 0.0;
            session.minutesRemaining = 0.0;
            session.socPercentage = Math.min(100.0, session.socPercentage);
            session.completedReady = true;
            return; // freeze energy and cost
        }

        // Use the full power of the specific gun (maxPowerKw)
        // In real hardware, guns are often rated for half the dispensary's total (e.g. 240kW dispensary has two 120kW guns)
        double availablePower = session.maxPowerKw;
        
        // SoC Tapering Logic: Slow down after 80% to protect battery
        if (session.socPercentage >= 80.0) {
            double taperFactor = 1.0 - ((session.socPercentage - 80.0) / 20.0); // Drops to 0 at 100%
            availablePower *= Math.max(0.1, taperFactor);
        }

        // 3. V-I Relationship Math with grid noise
        double noise = ThreadLocalRandom.current().nextDouble(0.98, 1.02);
        session.voltageV = 380.0 + ThreadLocalRandom.current().nextDouble(-2.0, 5.0); 
        session.currentA = (availablePower * noise * 1000.0) / session.voltageV;
        session.powerKw = (session.voltageV * session.currentA) / 1000.0;

        // 4. Energy Accumulation (kWh) — scaled by simulationSpeed so a full charge
        // can be demoed in minutes instead of hours (1.0 = realtime).
        double addedEnergy = ((session.powerKw * 5.0) / 3600.0) * simulationSpeed;
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
            // Divide by simulationSpeed so the displayed ETA matches the accelerated pace.
            session.minutesRemaining = (((session.batteryCapacityKwh * (1.0 - (session.socPercentage / 100.0))) / session.powerKw) * 60.0) / simulationSpeed;
        } else {
            session.minutesRemaining = 0;
        }
    }

    private void broadcastTelemetry(SimulatedSession session) {
        // Topic 1: Private (Slimmed down for the mobile user)
        Map<String, Object> userUpdate = new java.util.HashMap<>();
        userUpdate.put("bookingId", session.bookingId);
        userUpdate.put("slotId", session.slotId);
        userUpdate.put("stationId", session.stationId);
        userUpdate.put("powerKw", session.powerKw);
        userUpdate.put("energyDispensedKwh", session.energyDispensedKwh);
        userUpdate.put("socPercentage", session.socPercentage);
        userUpdate.put("totalCost", session.totalCost);
        userUpdate.put("minutesRemaining", session.minutesRemaining);
        userUpdate.put("maxPowerKw", session.maxPowerKw);
        userUpdate.put("batteryCapacityKwh", session.batteryCapacityKwh);
        userUpdate.put("pricePerKwh", session.pricePerKwh);
        // Signals the app to tear down telemetry and move to the payment screen.
        userUpdate.put("completed", session.completedReady);

        messagingTemplate.convertAndSend("/topic/session/" + session.bookingId, userUpdate);

        // Topic 2: Public (Aggregated for station searchers)
        Map<String, Object> publicUpdate = new java.util.HashMap<>();
        publicUpdate.put("bookingId", session.bookingId);
        publicUpdate.put("slotId", session.slotId);
        publicUpdate.put("stationId", session.stationId);
        publicUpdate.put("powerKw", session.powerKw);
        publicUpdate.put("energyDispensedKwh", session.energyDispensedKwh);
        publicUpdate.put("socPercentage", session.socPercentage);
        publicUpdate.put("totalCost", session.totalCost);
        publicUpdate.put("minutesRemaining", session.minutesRemaining);
        publicUpdate.put("maxPowerKw", session.maxPowerKw);
        publicUpdate.put("batteryCapacityKwh", session.batteryCapacityKwh);
        publicUpdate.put("pricePerKwh", session.pricePerKwh);
        publicUpdate.put("completed", session.completedReady);

        messagingTemplate.convertAndSend("/topic/station/" + session.stationId, publicUpdate);

        // Topic 3: Owner (Full health metrics for the dashboard)
        Map<String, Object> healthUpdate = Map.of(
            "slotId", session.slotId,
            "temp", String.format("%.1f°C", session.connectorTempC),
            "voltage", Math.round(session.voltageV),
            "current", Math.round(session.currentA),
            "power", session.powerKw,
            "energy", session.energyDispensedKwh,
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
            .startedAt(java.time.LocalDateTime.now())
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

        // Snapshot cadence counter (not broadcast)
        @Builder.Default
        private int ticksSinceSnapshot = 0;

        // Session start (for the max-duration cap) and full/complete flag
        private java.time.LocalDateTime startedAt;
        @Builder.Default
        private boolean completedReady = false;
    }
}
