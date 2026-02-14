package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.ChargingSessionRequest;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.controller.WebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private WebSocketController webSocketController;

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
            bookingRepository.save(booking);

            // Update slot status
            ChargerSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.CHARGING);
            slotRepository.save(slot);

            // Create charging session
            ChargingSession session = ChargingSession.builder()
                    .booking(booking)
                    .startTime(LocalDateTime.now())
                    .energyKwh(0.0)
                    .totalCost(0.0)
                    .build();

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // Notify via WebSocket
            webSocketController.notifySlotStatusChange(slot.getStation().getId(), slot);
            webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), booking);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Charging started successfully")
                    .data(savedSession)
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

            // Calculate energy and cost
            LocalDateTime endTime = LocalDateTime.now();
            double hours = java.time.Duration.between(session.getStartTime(), endTime).toMinutes() / 60.0;
            double powerRating = session.getBooking().getSlot().getPowerKw();
            double energyConsumed = hours * powerRating; // kWh
            double cost = energyConsumed * 15.0; // Rs. 15 per kWh

            session.setEndTime(endTime);
            session.setEnergyKwh(energyConsumed);
            session.setTotalCost(cost);

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // Update booking status
            Booking booking = session.getBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            // Update slot status
            ChargerSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);

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
