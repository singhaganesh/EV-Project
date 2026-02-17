package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.IoTSensorData;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.IoTSensorDataRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/iot")
public class IoTDataController {

    @Autowired
    private IoTSensorDataRepository ioTRepository;

    @Autowired
    private StationRepository stationRepository;

    // Endpoint for ESP32 to push data
    @PostMapping("/sensor-data")
    public ResponseEntity<APIResponse> receiveSensorData(@RequestBody Map<String, Object> data) {
        try {
            Long stationId = Long.parseLong(data.get("stationId").toString());
            Double voltage = Double.parseDouble(data.get("voltage").toString());
            Double current = Double.parseDouble(data.get("current").toString());

            // Calculate Power (Eq. 5: P = V * I)
            Double power = voltage * current;

            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new RuntimeException("Station not found"));

            IoTSensorData sensorData = IoTSensorData.builder()
                    .station(station)
                    .voltage(voltage)
                    .current(current)
                    .power(power)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Update Station's lastUsedTime
            station.setLastUsedTime(LocalDateTime.now());
            stationRepository.save(station);

            ioTRepository.save(sensorData);

            return ResponseEntity.ok(new APIResponse("Data received successfully", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new APIResponse("Error processing data: " + e.getMessage(), false));
        }
    }

    // Endpoint for Android app to view live data
    @GetMapping("/stations/{stationId}/live-power")
    public ResponseEntity<APIResponse> getLivePowerData(@PathVariable Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found"));

        Optional<IoTSensorData> latestData = ioTRepository.findTopByStationOrderByTimestampDesc(station);

        if (latestData.isPresent()) {
            return ResponseEntity.ok(new APIResponse(true, "Latest power data fetched", latestData.get()));
        } else {
            // Return dummy/simulated data if no sensor data exists yet (for demo/paper
            // simulation)
            Map<String, Object> dummyData = Map.of(
                    "stationId", stationId,
                    "voltage", 230.0,
                    "current", 0.0,
                    "power", 0.0,
                    "timestamp", LocalDateTime.now(),
                    "forecastedLoad", 0.0 // Eq. 6 placeholder
            );
            return ResponseEntity.ok(new APIResponse(true, "No live data, returning baseline", dummyData));
        }
    }
}
