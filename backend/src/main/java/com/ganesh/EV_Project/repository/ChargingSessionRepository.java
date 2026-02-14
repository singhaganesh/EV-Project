package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    boolean existsByBookingId(Long id);

    java.util.Optional<ChargingSession> findByBookingId(Long bookingId);

    java.util.List<ChargingSession> findByBookingUserId(Long userId);
}
