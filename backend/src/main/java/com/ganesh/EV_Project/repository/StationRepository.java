package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {
    boolean existsByName(String name);

    // Bounding box query — returns only stations visible in the current map
    // viewport
    @Query("SELECT s FROM Station s WHERE s.latitude BETWEEN :swLat AND :neLat AND s.longitude BETWEEN :swLng AND :neLng")
    List<Station> findByBoundingBox(
            @Param("swLat") double swLat,
            @Param("neLat") double neLat,
            @Param("swLng") double swLng,
            @Param("neLng") double neLng);
}
