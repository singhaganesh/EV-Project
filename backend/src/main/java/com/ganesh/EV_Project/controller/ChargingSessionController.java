package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.ChargingSessionRequest;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.controller.WebSocketController;
import com.ganesh.EV_Project.service.ChargingSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charging")
public class ChargingSessionController {

    @Autowired
    private ChargingSessionRepository chargingSessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ChargerSlotRepository slotRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private com.ganesh.EV_Project.repository.DispensaryRepository dispensaryRepository;

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private ChargingSimulatorService simulatorService;

    @PostMapping("/start")
    public ResponseEntity<?> startCharging(@RequestBody ChargingSessionRequest request) {
        try {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Verify booking is confirmed and start time has arrived
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body(APIResponse.builder()
                        .success(false)
                        .message("Booking is not confirmed")
                        .build());
            }

            // Check if session already exists
            if (chargingSessionRepository.existsByBookingId(booking.getId())) {
                return ResponseEntity.badRequest().body(APIResponse.builder()
                        .success(false)
                        .message("Charging session already exists for this booking")
                        .build());
            }

            // Update booking status
            booking.setStatus(BookingStatus.ONGOING);
            booking.setActualStartTime(LocalDateTime.now());
            bookingRepository.save(booking);

            // Update slot status
            ChargerSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.CHARGING);
            slotRepository.save(slot);

            // Create charging session
            ChargingSession session = ChargingSession.builder()
                    .booking(booking)
                    .startTime(LocalDateTime.now())
                    .status("ONGOING")
                    .energyKwh(0.0)
                    .totalCost(0.0)
                    .build();

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // ── TRIGGER SMART SIMULATION ──
            simulatorService.startSimulation(booking.getId());

            // Notify via WebSocket
            webSocketController.notifySlotStatusChange(slot.getStation().getId(), slot);
            webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), booking);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Charging started successfully")
                    .data(Map.of("id", savedSession.getId()))
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Failed to start charging: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/stop/{sessionId}")
    public ResponseEntity<?> stopCharging(@PathVariable Long sessionId) {
        try {
            ChargingSession session = chargingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Charging session not found"));

            if (session.getEndTime() != null) {
                return ResponseEntity.badRequest().body(APIResponse.builder()
                        .success(false)
                        .message("Charging session already ended")
                        .build());
            }

            // ── HALT SMART SIMULATION ──
            ChargingSimulatorService.SimulatedSession finalVitals = simulatorService.stopSimulation(session.getBooking().getId());

            // Calculate energy and cost
            LocalDateTime endTime = LocalDateTime.now();
            double hours = java.time.Duration.between(session.getStartTime(), endTime).toMinutes() / 60.0;
            double energyConsumed = (finalVitals != null) ? finalVitals.getEnergyDispensedKwh() : (hours * session.getBooking().getSlot().getPowerKw());
            double cost = (finalVitals != null) ? finalVitals.getTotalCost() : (energyConsumed * 15.0);

            session.setEndTime(endTime);
            session.setEnergyKwh(energyConsumed);
            session.setTotalCost(cost);
            session.setStatus("COMPLETED");

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // Release Booking and Slot
            Booking booking = session.getBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setActualEndTime(endTime);
            bookingRepository.save(booking);

            // Update slot status
            ChargerSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);

            // ── UPDATE STATION AND DISPENSARY LAST USED TIME ──
            Station station = slot.getStation();
            if (station != null) {
                station.setLastUsedTime(endTime);
                stationRepository.save(station);
            }
            
            com.ganesh.EV_Project.model.Dispensary dispensary = slot.getDispensary();
            if (dispensary != null) {
                dispensary.setLastUsedTime(endTime);
                dispensaryRepository.save(dispensary);
            }

            // Notify via WebSocket
            webSocketController.notifySlotStatusChange(slot.getStation().getId(), slot);
            webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), booking);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Charging completed successfully")
                    .data(savedSession)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Failed to stop charging: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId) {
        try {
            ChargingSession session = chargingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .data(session)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getSessionByBooking(@PathVariable Long bookingId) {
        try {
            ChargingSession session = chargingSessionRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new RuntimeException("Session not found for booking"));
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .data(session)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserChargingHistory(@PathVariable Long userId) {
        try {
            List<ChargingSession> sessions = chargingSessionRepository.findByBookingUserId(userId);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .data(sessions)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
