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
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.service.ChargingSimulatorService;
import com.ganesh.EV_Project.service.RazorpayService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private RazorpayService razorpayService;

    @Autowired
    private WebSocketController webSocketController;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Autowired
    private ChargingSimulatorService simulatorService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.ganesh.EV_Project.service.PushNotificationService pushNotificationService;

    @Autowired
    private com.ganesh.EV_Project.service.ChargingCompletionService completionService;

    /** True if the user owns the booking behind this session, or is an admin. */
    private boolean ownsBooking(User user, Booking booking) {
        return user != null && booking != null && booking.getUser() != null
                && (user.getRole() == User.Role.ADMIN
                    || booking.getUser().getId().equals(user.getId()));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.builder()
                .success(false)
                .message("Access denied: this resource does not belong to you")
                .build());
    }

    @PostMapping("/start")
    public ResponseEntity<?> startCharging(@jakarta.validation.Valid @RequestBody ChargingSessionRequest request,
                                           Authentication authentication) {
        try {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Ownership: caller must own the booking (or be admin)
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (!ownsBooking(currentUser, booking)) {
                return forbidden();
            }

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
                    .paymentStatus("PENDING")
                    .energyKwh(0.0)
                    .totalCost(0.0)
                    .build();

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // ── TRIGGER SMART SIMULATION ──
            simulatorService.startSimulation(booking.getId());

            // Notify via WebSocket - Wrapped in try-catch to prevent failure
            try {
                webSocketController.notifySlotStatusChange(slot.getStation().getId(), slot);
                webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), Map.of(
                    "bookingId", booking.getId(),
                    "status", booking.getStatus()
                ));
            } catch (Exception wsEx) {
                System.err.println("WebSocket Notification Failed: " + wsEx.getMessage());
            }

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
    public ResponseEntity<?> stopCharging(@PathVariable Long sessionId,
                                          Authentication authentication) {
        try {
            ChargingSession session = chargingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Charging session not found"));

            // Ownership: caller must own the session (or be admin)
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (!ownsBooking(currentUser, session.getBooking())) {
                return forbidden();
            }

            if (session.getEndTime() != null || "COMPLETED".equals(session.getStatus())) {
                return ResponseEntity.badRequest().body(APIResponse.builder()
                        .success(false)
                        .message("Charging session already ended")
                        .build());
            }

            // ── HALT SMART SIMULATION ──
            ChargingSimulatorService.SimulatedSession finalVitals = simulatorService.stopSimulation(session.getBooking().getId());

            // Finalize via the shared, idempotent service (same path the simulator
            // uses to auto-complete a full battery while the app is closed).
            Double finalEnergy = (finalVitals != null) ? finalVitals.getEnergyDispensedKwh() : null;
            Double finalCost = (finalVitals != null) ? finalVitals.getTotalCost() : null;
            ChargingSession savedSession = completionService.finalizeSession(session, finalEnergy, finalCost);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Charging completed successfully")
                    .data(Map.of(
                        "id", savedSession.getId(),
                        "session", savedSession,
                        "razorpayOrderId", savedSession.getRazorpayOrderId() != null ? savedSession.getRazorpayOrderId() : "",
                        "totalCost", savedSession.getTotalCost() != null ? savedSession.getTotalCost() : 0.0,
                        "razorpayKeyId", razorpayKeyId
                    ))
                    .build());

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in stopCharging: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Failed to stop charging: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId,
                                        Authentication authentication) {
        try {
            ChargingSession session = chargingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (!ownsBooking(currentUser, session.getBooking())) {
                return forbidden();
            }
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
    public ResponseEntity<?> getSessionByBooking(@PathVariable Long bookingId,
                                                 Authentication authentication) {
        try {
            ChargingSession session = chargingSessionRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new RuntimeException("Session not found for booking"));
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (!ownsBooking(currentUser, session.getBooking())) {
                return forbidden();
            }
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
    public ResponseEntity<?> getUserChargingHistory(@PathVariable Long userId,
                                                    Authentication authentication) {
        try {
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (currentUser == null
                    || (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(userId))) {
                return forbidden();
            }
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

    /** Completed-but-unpaid sessions for the in-app pending-payment recovery. */
    @GetMapping("/user/{userId}/outstanding")
    public ResponseEntity<?> getOutstandingSessions(@PathVariable Long userId,
                                                    Authentication authentication) {
        try {
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (currentUser == null
                    || (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(userId))) {
                return forbidden();
            }
            List<ChargingSession> sessions = chargingSessionRepository.findOutstandingByUser(userId);
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
