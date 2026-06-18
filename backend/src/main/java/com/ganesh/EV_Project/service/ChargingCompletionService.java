package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.controller.WebSocketController;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.DispensaryRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Single source of truth for ending a charging session, shared by the manual
 * stop endpoint and the simulator's auto-complete at 100% / overtime.
 *
 * Finalization is server-authoritative: it must run whether or not the user's
 * app is open, exactly like real charging hardware that stops dispensing when
 * the battery is full. Kept idempotent so a manual stop and an auto-complete
 * racing on the same session can't double-finalize or double-charge.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChargingCompletionService {

    private final ChargingSessionRepository chargingSessionRepository;
    private final BookingRepository bookingRepository;
    private final ChargerSlotRepository slotRepository;
    private final StationRepository stationRepository;
    private final DispensaryRepository dispensaryRepository;
    private final RazorpayService razorpayService;
    private final WebSocketController webSocketController;
    private final PushNotificationService pushNotificationService;

    /**
     * Marks the session COMPLETED, releases the booking, holds the slot for
     * payment, creates the Razorpay order, and notifies the user (WebSocket +
     * push). No-op if the session is already ended.
     *
     * @param finalEnergyKwh final energy (from the simulator), or null to derive from duration
     * @param finalCost      final cost (from the simulator), or null to derive
     * @return the finalized session (or the unchanged session if already ended)
     */
    @Transactional
    public ChargingSession finalizeSession(Long sessionId, Double finalEnergyKwh, Double finalCost) {
        // Re-load inside this transaction so the lazy associations (booking/slot/
        // station/dispensary/user) initialize safely. The simulator's @Scheduled
        // caller has no open-session-in-view, so a detached entity passed in here
        // would throw LazyInitializationException, roll back the finalize, and leave
        // the session stuck ONGOING while the app was told it completed.
        ChargingSession session = chargingSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return null;
        }
        // Idempotency guard — never finalize the same session twice.
        if (session.getEndTime() != null || "COMPLETED".equals(session.getStatus())) {
            return session;
        }

        Booking booking = session.getBooking();
        LocalDateTime endTime = LocalDateTime.now();
        double hours = Duration.between(session.getStartTime(), endTime).toMinutes() / 60.0;
        double energyConsumed = (finalEnergyKwh != null)
                ? finalEnergyKwh
                : hours * booking.getSlot().getPowerKw();
        double cost = MoneyUtil.round2((finalCost != null) ? finalCost : energyConsumed * 15.0);

        session.setEndTime(endTime);
        session.setEnergyKwh(energyConsumed);
        session.setTotalCost(cost);
        session.setStatus("COMPLETED");
        ChargingSession savedSession = chargingSessionRepository.save(session);

        // Release the booking; hold the slot in PAYMENT_PENDING until payment is
        // verified so another driver can't grab it before this one pays.
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setActualEndTime(endTime);
        bookingRepository.save(booking);

        ChargerSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.PAYMENT_PENDING);
        slotRepository.save(slot);

        Station station = slot.getStation();
        if (station != null) {
            station.setLastUsedTime(endTime);
            stationRepository.save(station);
        }
        Dispensary dispensary = slot.getDispensary();
        if (dispensary != null) {
            dispensary.setLastUsedTime(endTime);
            dispensaryRepository.save(dispensary);
        }

        // Create the Razorpay order (best-effort; never fail completion over it).
        try {
            String razorpayOrderId = razorpayService.createOrder(cost, booking.getId().toString());
            session.setRazorpayOrderId(razorpayOrderId);
            savedSession = chargingSessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for session {}: {}", session.getId(), e.getMessage());
        }

        // Notify any live UI (best-effort).
        try {
            if (station != null) {
                webSocketController.notifySlotStatusChange(station.getId(), slot);
            }
            if (booking.getUser() != null) {
                webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), Map.of(
                        "bookingId", booking.getId(),
                        "status", booking.getStatus()));
            }
        } catch (Exception e) {
            log.warn("WebSocket completion notify failed for session {}: {}", session.getId(), e.getMessage());
        }

        // Push reaches the driver who walked away; deeplink opens the payment screen.
        // Best-effort: a push failure must never roll back the finalize.
        if (booking.getUser() != null) {
            try {
                pushNotificationService.sendToUser(
                        booking.getUser().getId(),
                        "CHARGING_COMPLETE",
                        "Charging complete",
                        "Your session is done. Amount due: ₹" + cost + ".",
                        "plugsy://payment/" + savedSession.getId());
            } catch (Exception e) {
                log.warn("Completion push failed for session {}: {}", session.getId(), e.getMessage());
            }
        }

        return savedSession;
    }
}
