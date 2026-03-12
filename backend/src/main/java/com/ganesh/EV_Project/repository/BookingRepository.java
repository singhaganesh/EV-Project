package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.enums.BookingStatus;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
       List<Booking> findByUserId(Long userId);

       List<Booking> findByStatus(BookingStatus bookingStatus);

       void deleteBySlot(com.ganesh.EV_Project.model.ChargerSlot slot);

       // Check for overlapping bookings for a specific slot (kept for backward compatibility)
       @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId " +
                     "AND b.status IN ('CONFIRMED', 'ONGOING') " +
                     "AND ((b.startTime <= :endTime AND b.endTime >= :startTime))")
       List<Booking> findOverlappingBookings(@Param("slotId") Long slotId,
                     @Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

       // Find the earliest ending active booking for "next available" suggestion
       @Query("SELECT b FROM Booking b WHERE b.slot.station.id = :stationId " +
                     "AND b.slot.connectorType = :connectorType " +
                     "AND b.status IN ('CONFIRMED', 'ONGOING') " +
                     "ORDER BY b.expiresAt ASC")
       List<Booking> findActiveBookingsByStationAndConnector(
                     @Param("stationId") Long stationId,
                     @Param("connectorType") ConnectorType connectorType);
}
