package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.BookingRequest;
import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.SlotStatus;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ChargerSlotRepository slotRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChargingSessionRepository chargingSessionRepository;


    @Transactional
    public Booking createBooking(BookingRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new APIException("User not found"));

        ChargerSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new APIException("Slot not found"));

        // Check if requested start time is in the past
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new APIException("Start time cannot be in the past");
        }

        // Check if end time is before start time
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new APIException("End time cannot be before start time");
        }

        // Check if slot is available
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new APIException("Slot not available for booking");
        }
        
        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                request.getSlotId(), request.getStartTime(), request.getEndTime());
        
        if (!overlappingBookings.isEmpty()) {
            throw new APIException("Slot is already booked for the selected time range");
        }

        // Calculate price estimate (Rs. 15 per kWh, assuming 1 hour charging)
        double hours = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes() / 60.0;
        double priceEstimate = hours * slot.getPowerKw() * 15.0; // Rs. 15 per kWh

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .slot(slot)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.CONFIRMED)
                .priceEstimate(priceEstimate)
                .build();

        // Update slot status
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        return bookingRepository.save(booking);
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

    @Scheduled(fixedRate = 60000) // runs every 1 minute
    @Transactional
    public void expireUnstartedBookings() {
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            boolean hasSession = chargingSessionRepository.existsByBookingId(booking.getId());

            if (LocalDateTime.now().isAfter(booking.getStartTime().plusMinutes(15)) && !hasSession) {
                booking.setStatus(BookingStatus.EXPIRED);
                booking.getSlot().setStatus(SlotStatus.AVAILABLE);
                bookingRepository.save(booking);
            }
        }
    }
}
