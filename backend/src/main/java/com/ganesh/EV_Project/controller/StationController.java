package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping
    public ResponseEntity<List<Station>> getAllStations() {
        List<Station> allStations = stationService.getAllStations();
        System.out.println(allStations.toString());
        return new ResponseEntity<>(allStations, HttpStatus.OK);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Station>> getStationsByOwnerId(@PathVariable Long ownerId) {
        List<Station> ownerStations = stationService.getStationsByOwnerId(ownerId);
        return new ResponseEntity<>(ownerStations, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.ganesh.EV_Project.payload.APIResponse> getStationById(@PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return new ResponseEntity<>(
                new com.ganesh.EV_Project.payload.APIResponse(true, "Station fetched successfully", station),
                HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Station> addStation(@RequestBody Station station) {

        Station savedStation = stationService.addStation(station);

        return new ResponseEntity<>(savedStation, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Station> updateStation(@PathVariable Long id, @RequestBody Station station) {
        Station updatedStation = stationService.updateStation(id, station);
        return new ResponseEntity<>(updatedStation, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return new ResponseEntity<>("Station deleted successfully", HttpStatus.OK);
    }
}
