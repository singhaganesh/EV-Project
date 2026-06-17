package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.FavoriteStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteStationRepository extends JpaRepository<FavoriteStation, Long> {
    List<FavoriteStation> findByUserId(Long userId);
    Optional<FavoriteStation> findByUserIdAndStationId(Long userId, Long stationId);
    boolean existsByUserIdAndStationId(Long userId, Long stationId);
}
