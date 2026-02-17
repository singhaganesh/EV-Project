package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.IoTSensorData;
import com.ganesh.EV_Project.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IoTSensorDataRepository extends JpaRepository<IoTSensorData, Long> {

    Optional<IoTSensorData> findTopByStationOrderByTimestampDesc(Station station);

    List<IoTSensorData> findTop10ByStationOrderByTimestampDesc(Station station);
}
