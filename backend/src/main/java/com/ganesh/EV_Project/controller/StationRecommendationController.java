package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.StationMarkerDTO;
import com.ganesh.EV_Project.dto.StationScoreDTO;
import com.ganesh.EV_Project.service.StationRecommendationService;
import com.ganesh.EV_Project.payload.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationRecommendationController {

    @Autowired
    private StationRecommendationService recommendationService;

    // Lightweight markers for map viewport (handles 50K+ stations efficiently)
    @GetMapping("/viewport")
    public ResponseEntity<APIResponse> getStationsInViewport(
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam double swLat,
            @RequestParam double swLng) {

        List<StationMarkerDTO> markers = recommendationService.getStationsInViewport(swLat, neLat, swLng, neLng);
        return ResponseEntity.ok(new APIResponse(true, "Viewport stations fetched", markers));
    }

    // Top N nearest stations with full scoring (for bottom pager/list)
    @GetMapping("/nearby")
    public ResponseEntity<APIResponse> getNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50.0") double radius,
            @RequestParam(defaultValue = "5") int limit) {

        List<StationScoreDTO> stations = recommendationService.getNearbyStationsRanked(lat, lng, radius, limit);
        return ResponseEntity.ok(new APIResponse(true, "Nearby stations fetched successfully", stations));
    }

    // Full detail for a single station (on marker click)
    @GetMapping("/{id}/detail")
    public ResponseEntity<APIResponse> getStationDetail(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng) {

        StationScoreDTO detail = recommendationService.getStationDetail(id, lat, lng);
        return ResponseEntity.ok(new APIResponse(true, "Station detail fetched", detail));
    }
}
