package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.controller.WebSocketController;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.util.MoneyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChargerSlotService {

    @Autowired
    private ChargerSlotRepository slotRepository;
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ChargingSessionRepository chargingSessionRepository;
    @Autowired
    private ChargingSimulatorService simulatorService;
    @Autowired
    private RazorpayService razorpayService;
    @Autowired
    private WebSocketController webSocketController;
    @Autowired
    private PushNotificationService pushNotificationService;

    public List<ChargerSlot> getAllSlots() {
        return slotRepository.findAll();
    }

    public List<ChargerSlot> getSlotsByStation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        return slotRepository.findByStation(station);
    }

    public List<ChargerSlot> getAvailableSlots(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        return slotRepository.findByStationAndStatus(station, SlotStatus.AVAILABLE);
    }

    @Transactional
    public ChargerSlot updateSlotStatus(Long id, SlotStatus status) {
        ChargerSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new APIException("Slot not found"));

        // If an owner takes a charging gun offline, force-stop and bill the
        // active session and notify the driver before flipping to maintenance.
        if (status == SlotStatus.MAINTENANCE && slot.getStatus() == SlotStatus.CHARGING) {
            forceStopActiveSession(slot);
        }

        slot.setStatus(status);
        ChargerSlot saved = slotRepository.save(slot);
        try {
            if (slot.getStation() != null) {
                webSocketController.notifySlotStatusChange(slot.getStation().getId(), saved);
            }
        } catch (Exception e) {
            log.warn("WebSocket slot status notify failed: {}", e.getMessage());
        }
        return saved;
    }

    private void forceStopActiveSession(ChargerSlot slot) {
        chargingSessionRepository.findOngoingBySlot(slot.getId()).ifPresent(session -> {
            Booking booking = session.getBooking();
            ChargingSimulatorService.SimulatedSession finalVitals =
                    simulatorService.stopSimulation(booking.getId());

            LocalDateTime endTime = LocalDateTime.now();
            double energy = finalVitals != null ? finalVitals.getEnergyDispensedKwh()
                    : (session.getEnergyKwh() != null ? session.getEnergyKwh() : 0.0);
            double cost = MoneyUtil.round2(finalVitals != null ? finalVitals.getTotalCost()
                    : (session.getTotalCost() != null ? session.getTotalCost() : 0.0));

            session.setEndTime(endTime);
            session.setEnergyKwh(energy);
            session.setTotalCost(cost);
            session.setStatus("COMPLETED");

            String razorpayOrderId = null;
            try {
                razorpayOrderId = razorpayService.createOrder(cost, booking.getId().toString());
                session.setRazorpayOrderId(razorpayOrderId);
            } catch (Exception e) {
                log.error("Failed to create Razorpay order for maintenance stop: {}", e.getMessage());
            }
            chargingSessionRepository.save(session);

            booking.setStatus(BookingStatus.COMPLETED);
            booking.setActualEndTime(endTime);
            bookingRepository.save(booking);

            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("bookingId", booking.getId());
                payload.put("status", "FORCE_STOPPED_MAINTENANCE");
                payload.put("message", "Charging was stopped because the charger entered maintenance. Please complete payment.");
                payload.put("totalCost", cost);
                payload.put("razorpayOrderId", razorpayOrderId != null ? razorpayOrderId : "");
                if (booking.getUser() != null) {
                    webSocketController.notifyUserBookingUpdate(booking.getUser().getId(), payload);
                }
            } catch (Exception e) {
                log.warn("WebSocket driver notify failed: {}", e.getMessage());
            }

            // ── PUSH: charging force-stopped for maintenance & billed ──
            if (booking.getUser() != null) {
                pushNotificationService.sendToUser(
                        booking.getUser().getId(),
                        "FORCE_STOPPED",
                        "Charging stopped",
                        "Your charging was stopped for maintenance. Please complete payment.",
                        "plugsy://payment/" + session.getId());
            }
        });
    }

    public ChargerSlot updateSlotConnectorType(Long id, ConnectorType connectorType) {
        ChargerSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new APIException("Slot not found"));
        slot.setConnectorType(connectorType);
        return slotRepository.save(slot);
    }

    public ChargerSlot createSlot(ChargerSlot slot, Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        slot.setStation(station);
        slot.setStatus(SlotStatus.AVAILABLE);
        return slotRepository.save(slot);
    }

    public ChargerSlot getSlotById(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new APIException("Slot not found"));
    }
}
