package com.ganesh.EV_Project.dto;

import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.VehicleType;

import java.time.LocalDateTime;

/** Flat projection of a booking for the owner's read-only bookings view. */
public record OwnerBookingRowDTO(
    Long id,
    String customerName,
    String stationName,
    String slotLabel,
    VehicleType vehicleType,
    BookingStatus status,
    LocalDateTime startTime,
    LocalDateTime expiresAt
) {}
