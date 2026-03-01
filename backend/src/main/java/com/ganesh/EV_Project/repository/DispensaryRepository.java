package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.Dispensary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispensaryRepository extends JpaRepository<Dispensary, Long> {
    List<Dispensary> findByStationId(Long stationId);
}
