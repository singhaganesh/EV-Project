package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.BookingRequest;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.VehicleType;
import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    @Value("${app.booking.expiration-minutes:20}")
    private int expirationMinutes;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ChargerSlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChargingSessionRepository chargingSessionRepository;

    /**
     * "Book Now" — instant booking with random connector assignment.
     *
     * Flow:
     * 1. Lock & fetch available connectors matching station + connectorType
     * 2. Filter for truck support if vehicleType is TRUCK
     * 3. If no candidates → find earliest "next available" time and throw error
     * 4. Random pick from candidates
     * 5. Create booking with server-set startTime = NOW, expiresAt = NOW + 20 min
     * 6. Set slot status to BOOKED
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        // Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new APIException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
        ChargerSlot assignedSlot;

        // Admin override: if slotId is provided, use it directly
        if (request.getSlotId() != null) {
            assignedSlot = slotRepository.findById(request.getSlotId())
                    .orElseThrow(() -> new APIException("Slot not found"));
            if (assignedSlot.getStatus() != SlotStatus.AVAILABLE) {
                throw new APIException("Selected slot is not available");
            }
        } else {
            // ── Smart Assignment with Pessimistic Locking ──
            // This query locks the rows to prevent race conditions
            List<ChargerSlot> candidates = slotRepository.findAvailableSlotsForUpdate(
                    request.getStationId(), request.getConnectorType());

            // Filter for truck support if needed
            if (request.getVehicleType() == VehicleType.TRUCK) {
                // Trucks must ONLY use truck-capable slots
                candidates = candidates.stream()
                        .filter(s -> s.getDispensary() != null
                                && Boolean.TRUE.equals(s.getDispensary().getAcceptsTrucks()))
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    String nextAvailable = getNextAvailableTime(request.getStationId(), request.getConnectorType());
                    throw new APIException("No " + request.getConnectorType() + " truck connectors available. " + nextAvailable);
                }
            } else if (request.getVehicleType() == VehicleType.CAR) {
                // Cars preferably use car-only slots, but CAN use truck slots if allowed
                List<ChargerSlot> carSlots = candidates.stream()
                        .filter(s -> s.getDispensary() == null || !Boolean.TRUE.equals(s.getDispensary().getAcceptsTrucks()))
                        .collect(Collectors.toList());

                List<ChargerSlot> truckSlots = candidates.stream()
                        .filter(s -> s.getDispensary() != null && Boolean.TRUE.equals(s.getDispensary().getAcceptsTrucks()))
                        .collect(Collectors.toList());

                if (!carSlots.isEmpty()) {
                    // Car slots are available! Pick one of these first.
                    candidates = carSlots;
                } else if (!truckSlots.isEmpty()) {
                    // No car slots, but truck slots are available.
                    if (request.isAllowTruckSlotFallback()) {
                        candidates = truckSlots; // User opted-in to the truck slot
                    } else {
                        throw new APIException("PROMPT_TRUCK_FALLBACK"); // Frontend must catch this exact string
                    }
                } else {
                    // Truly no slots left
                    String nextAvailable = getNextAvailableTime(request.getStationId(), request.getConnectorType());
                    throw new APIException("No " + request.getConnectorType() + " connectors available. " + nextAvailable);
                }
            }

            // Random pick
            Collections.shuffle(candidates);
            assignedSlot = candidates.get(0);
        }

        // Create booking — startTime and expiresAt are server-set, NOT user-provided
        Booking booking = Booking.builder()
                .user(user)
                .slot(assignedSlot)
                .startTime(now)
                .endTime(now) // placeholder; actual end = when user stops charging
                .status(BookingStatus.CONFIRMED)
                .priceEstimate(0.0) // calculated after charging completes
                .vehicleType(request.getVehicleType())
                .expiresAt(expiresAt)
                .build();

        // Update slot status
        assignedSlot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(assignedSlot);

        return bookingRepository.save(booking);
    }

    /**
     * Find when the next connector of this type will be free.
     * Returns a user-friendly message like "Next available: ~2:45 PM"
     */
    private String getNextAvailableTime(Long stationId,
            com.ganesh.EV_Project.enums.ConnectorType connectorType) {
        List<Booking> activeBookings = bookingRepository
                .findActiveBookingsByStationAndConnector(stationId, connectorType);

        if (activeBookings.isEmpty()) {
            return "Please try again shortly.";
        }

        // The first result is the earliest expiresAt (sorted ASC in the query)
        Booking earliest = activeBookings.get(0);
        LocalDateTime freeAt = earliest.getExpiresAt() != null
                ? earliest.getExpiresAt()
                : earliest.getStartTime().plusMinutes(expirationMinutes);

        String formattedTime = freeAt.format(DateTimeFormatter.ofPattern("h:mm a"));
        return "Next available: ~" + formattedTime;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking not found"));

        // Only allow cancelling confirmed bookings
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new APIException("Cannot cancel booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Make slot available again
        ChargerSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);
    }

    /**
     * Auto-expire unstarted bookings after grace period (20 min).
     * Runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnstartedBookings() {
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            boolean hasSession = chargingSessionRepository.existsByBookingId(booking.getId());

            LocalDateTime expiration = booking.getExpiresAt() != null ? booking.getExpiresAt()
                    : booking.getStartTime().plusMinutes(expirationMinutes);

            if (LocalDateTime.now().isAfter(expiration) && !hasSession) {
                booking.setStatus(BookingStatus.EXPIRED);
                booking.getSlot().setStatus(SlotStatus.AVAILABLE);
                bookingRepository.save(booking);
            }
        }
    }
}
