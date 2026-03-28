package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.OwnerStationStatsDTO;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.StationService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private StationService stationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Station>> getAllStations() {
        List<Station> allStations = stationService.getAllStations();
        return new ResponseEntity<>(allStations, HttpStatus.OK);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> getStationsByOwnerId(@PathVariable Long ownerId, Authentication authentication) {
        // Secure check: Verify requester is the owner or an admin
        String principal = authentication.getName();
        User currentUser = userService.findByPhoneNumber(principal);
        if (currentUser == null) currentUser = userService.findByEmail(principal);

        if (currentUser == null || (!currentUser.getRole().equals(User.Role.ADMIN) && !currentUser.getId().equals(ownerId))) {
            return new ResponseEntity<>("Access Denied: You cannot view other owners' stations", HttpStatus.FORBIDDEN);
        }

        List<Station> ownerStations = stationService.getStationsByOwnerId(ownerId);
        return new ResponseEntity<>(ownerStations, HttpStatus.OK);
    }

    @GetMapping("/owner/{ownerId}/stats")
    public ResponseEntity<?> getOwnerRealtimeStats(@PathVariable Long ownerId, Authentication authentication) {
        // Secure check: Verify requester is the owner or an admin
        String principal = authentication.getName();
        User currentUser = userService.findByPhoneNumber(principal);
        if (currentUser == null) currentUser = userService.findByEmail(principal);

        if (currentUser == null || (!currentUser.getRole().equals(User.Role.ADMIN) && !currentUser.getId().equals(ownerId))) {
            return new ResponseEntity<>("Access Denied: You cannot view these statistics", HttpStatus.FORBIDDEN);
        }

        OwnerStationStatsDTO stats = stationService.getOwnerRealtimeStats(ownerId);
        return new ResponseEntity<>(
                new APIResponse(true, "Owner station stats fetched successfully", stats),
                HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse> getStationById(@PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return new ResponseEntity<>(
                new APIResponse(true, "Station fetched successfully", station),
                HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<Station> addStation(@RequestBody Station station) {
        Station savedStation = stationService.addStation(station);
        return new ResponseEntity<>(savedStation, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<Station> updateStation(@PathVariable Long id, @RequestBody Station station) {
        // Note: In production, we should verify the station belongs to the authenticated user
        Station updatedStation = stationService.updateStation(id, station);
        return new ResponseEntity<>(updatedStation, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<String> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return new ResponseEntity<>("Station deleted successfully", HttpStatus.OK);
    }
}
