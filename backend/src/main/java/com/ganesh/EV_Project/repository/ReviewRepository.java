package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStationIdOrderByCreatedAtDesc(Long stationId);

    Optional<Review> findByUserIdAndStationId(Long userId, Long stationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.stationId = :stationId")
    Double averageRating(@Param("stationId") Long stationId);

    long countByStationId(Long stationId);
}
