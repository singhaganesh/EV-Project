package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    boolean existsByBookingId(Long id);

    java.util.Optional<ChargingSession> findByBookingId(Long bookingId);

    java.util.Optional<ChargingSession> findByRazorpayOrderId(String orderId);

    java.util.List<ChargingSession> findByBookingUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.station.id = :stationId AND s.status = 'COMPLETED'")
    java.time.LocalDateTime findLatestSessionTimeByStation(@org.springframework.data.repository.query.Param("stationId") Long stationId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.dispensary.id = :dispensaryId AND s.status = 'COMPLETED'")
    java.time.LocalDateTime findLatestSessionTimeByDispensary(@org.springframework.data.repository.query.Param("dispensaryId") Long dispensaryId);

    @org.springframework.data.jpa.repository.Query("SELECT s.booking.slot.station.id, MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.station.id IN :stationIds AND s.status = 'COMPLETED' GROUP BY s.booking.slot.station.id")
    List<Object[]> findLatestSessionTimesByStationIds(@org.springframework.data.repository.query.Param("stationIds") List<Long> stationIds);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.energyKwh) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.endTime >= :since AND s.status = 'COMPLETED'")
    Double sumEnergyByOwnerSince(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.endTime >= :since AND s.status = 'COMPLETED'")
    Double sumEarningsByOwnerSince(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
