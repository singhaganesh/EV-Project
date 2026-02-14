package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerSlotRepository extends JpaRepository<ChargerSlot, Long> {
    List<ChargerSlot> findByStation(Station station);
    List<ChargerSlot> findByStatus(String status);
    List<ChargerSlot> findByStationAndStatus(Station station, SlotStatus status);
}


