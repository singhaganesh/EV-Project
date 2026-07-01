package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    boolean existsByBookingId(Long id);

    java.util.List<ChargingSession> findByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChargingSession s " +
            "WHERE s.paymentStatus <> 'PAID' AND s.status = 'COMPLETED' AND s.endTime < :cutoff")
    java.util.List<ChargingSession> findUnpaidEndedBefore(
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);

    java.util.Optional<ChargingSession> findByBookingId(Long bookingId);

    // Eagerly loads the booking/user/slot/station chain so a receipt can be
    // rendered outside an open Hibernate session (no LazyInitializationException).
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChargingSession s " +
            "JOIN FETCH s.booking b JOIN FETCH b.user JOIN FETCH b.slot sl JOIN FETCH sl.station " +
            "WHERE s.id = :id")
    java.util.Optional<ChargingSession> findByIdWithDetails(
            @org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChargingSession s " +
            "WHERE s.booking.slot.id = :slotId AND s.status = 'ONGOING'")
    java.util.Optional<ChargingSession> findOngoingBySlot(
            @org.springframework.data.repository.query.Param("slotId") Long slotId);

    java.util.Optional<ChargingSession> findByRazorpayOrderId(String orderId);

    // Whether a user has ever completed a charging session at a station — gates
    // who may post a review (F2).
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) > 0 FROM ChargingSession s " +
            "WHERE s.booking.user.id = :userId AND s.booking.slot.station.id = :stationId " +
            "AND s.status = 'COMPLETED'")
    boolean hasCompletedSession(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("stationId") Long stationId);

    java.util.List<ChargingSession> findByBookingUserId(Long userId);

    // Completed-but-unpaid sessions for the in-app "pending payment" recovery.
    // JOIN FETCH the chain so the station name is available for the banner.
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChargingSession s " +
            "JOIN FETCH s.booking b JOIN FETCH b.slot sl JOIN FETCH sl.station " +
            "WHERE b.user.id = :userId AND s.status = 'COMPLETED' AND s.paymentStatus <> 'PAID' " +
            "ORDER BY s.endTime DESC")
    java.util.List<ChargingSession> findOutstandingByUser(
            @org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.station.id = :stationId AND s.status = 'COMPLETED'")
    java.time.LocalDateTime findLatestSessionTimeByStation(@org.springframework.data.repository.query.Param("stationId") Long stationId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.dispensary.id = :dispensaryId AND s.status = 'COMPLETED'")
    java.time.LocalDateTime findLatestSessionTimeByDispensary(@org.springframework.data.repository.query.Param("dispensaryId") Long dispensaryId);

    @org.springframework.data.jpa.repository.Query("SELECT s.booking.slot.station.id, MAX(s.endTime) FROM ChargingSession s WHERE s.booking.slot.station.id IN :stationIds AND s.status = 'COMPLETED' GROUP BY s.booking.slot.station.id")
    List<Object[]> findLatestSessionTimesByStationIds(@org.springframework.data.repository.query.Param("stationIds") List<Long> stationIds);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.energyKwh) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.endTime >= :since AND s.status = 'COMPLETED'")
    Double sumEnergyByOwnerSince(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.endTime >= :since AND s.paymentStatus = 'PAID'")
    Double sumEarningsByOwnerSince(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    // Hour-aligned trend support: totals between two explicit timestamps.
    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.energyKwh) FROM ChargingSession s " +
            "WHERE s.booking.slot.station.owner.id = :ownerId " +
            "AND s.endTime >= :start AND s.endTime <= :end AND s.status = 'COMPLETED'")
    Double sumEnergyByOwnerBetween(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s " +
            "WHERE s.booking.slot.station.owner.id = :ownerId " +
            "AND s.endTime >= :start AND s.endTime <= :end AND s.paymentStatus = 'PAID'")
    Double sumEarningsByOwnerBetween(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(value = "SELECT CAST(s.end_time AS DATE) as date, SUM(s.total_cost) as revenue, SUM(s.energy_kwh) as energy " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id JOIN stations st ON cs.station_id = st.id " +
           "WHERE st.owner_id = :ownerId AND s.payment_status = 'PAID' AND s.end_time >= :since " +
           "GROUP BY CAST(s.end_time AS DATE) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getDailyStatsByOwner(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXTRACT(HOUR FROM s.start_time) as hour, COUNT(*) as count " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id JOIN stations st ON cs.station_id = st.id " +
           "WHERE st.owner_id = :ownerId AND s.start_time >= :since " +
           "GROUP BY hour ORDER BY hour ASC", nativeQuery = true)
    List<Object[]> getPeakUsageByOwner(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
           "AVG(EXTRACT(EPOCH FROM (s.end_time - s.start_time))/60) as avgDuration, " +
           "AVG(s.total_cost) as avgRevenue, " +
           "AVG(s.energy_kwh) as avgEnergy " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id JOIN stations st ON cs.station_id = st.id " +
           "WHERE st.owner_id = :ownerId AND s.payment_status = 'PAID' AND s.end_time >= :since", nativeQuery = true)
    List<Object[]> getEfficiencyMetrics(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(value = "SELECT st.name, SUM(s.total_cost) as revenue " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id JOIN stations st ON cs.station_id = st.id " +
           "WHERE st.owner_id = :ownerId AND s.payment_status = 'PAID' AND s.end_time >= :since " +
           "GROUP BY st.name", nativeQuery = true)
    List<Object[]> getRevenueByStation(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(value = "SELECT cs.connector_type, SUM(s.total_cost) as revenue " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id JOIN stations st ON cs.station_id = st.id " +
           "WHERE st.owner_id = :ownerId AND s.payment_status = 'PAID' AND s.end_time >= :since " +
           "GROUP BY cs.connector_type", nativeQuery = true)
    List<Object[]> getRevenueByConnector(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.paymentStatus = 'PAID'")
    Double getTotalLifetimeRevenue(@org.springframework.data.repository.query.Param("ownerId") Long ownerId);

    // Total cost of energy sold = sum of (kWh × the station's grid tariff) over paid sessions.
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.energyKwh * COALESCE(s.booking.slot.station.costPerKwh, 0)), 0) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.paymentStatus = 'PAID'")
    Double getTotalEnergyCost(@org.springframework.data.repository.query.Param("ownerId") Long ownerId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalCost) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.paymentStatus = 'PAID' AND s.endTime >= :since")
    Double getRecentRevenue(@org.springframework.data.repository.query.Param("ownerId") Long ownerId, @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT new com.ganesh.EV_Project.dto.TransactionRowDTO(" +
           "s.id, s.endTime, s.booking.slot.station.name, s.energyKwh, s.totalCost, s.razorpayOrderId, s.paymentStatus) " +
           "FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId " +
           "AND (CAST(:search AS string) IS NULL OR LOWER(s.booking.slot.station.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.razorpayOrderId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY s.endTime DESC")
    org.springframework.data.domain.Page<com.ganesh.EV_Project.dto.TransactionRowDTO> getTransactionHistory(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    // Non-paged variant for CSV export (full ledger, newest first).
    @org.springframework.data.jpa.repository.Query("SELECT new com.ganesh.EV_Project.dto.TransactionRowDTO(" +
           "s.id, s.endTime, s.booking.slot.station.name, s.energyKwh, s.totalCost, s.razorpayOrderId, s.paymentStatus) " +
           "FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId " +
           "ORDER BY s.endTime DESC")
    java.util.List<com.ganesh.EV_Project.dto.TransactionRowDTO> getAllTransactions(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
           "COUNT(*) as totalSessions, " +
           "COALESCE(SUM(s.energy_kwh), 0.0) as totalEnergy, " +
           "COALESCE(AVG(EXTRACT(EPOCH FROM (s.end_time - s.start_time))/60), 0.0) as avgDuration " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id " +
           "WHERE cs.dispensary_id = :dispensaryId AND s.status = 'COMPLETED'", nativeQuery = true)
    List<Object[]> getDispensaryMetrics(@org.springframework.data.repository.query.Param("dispensaryId") Long dispensaryId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXTRACT(HOUR FROM s.start_time) as hour, COUNT(*) as count " +
           "FROM charging_sessions s JOIN bookings b ON s.booking_id = b.id " +
           "JOIN charger_slots cs ON b.slot_id = cs.id " +
           "WHERE cs.dispensary_id = :dispensaryId AND s.status = 'COMPLETED' " +
           "GROUP BY hour ORDER BY hour ASC", nativeQuery = true)
    List<Object[]> getPeakUsageByDispensary(@org.springframework.data.repository.query.Param("dispensaryId") Long dispensaryId);
}
