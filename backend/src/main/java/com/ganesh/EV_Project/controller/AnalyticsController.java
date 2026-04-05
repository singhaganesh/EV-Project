package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.AnalyticsSummaryDTO;
import com.ganesh.EV_Project.dto.DailyStatsDTO;
import com.ganesh.EV_Project.dto.PeakHourDTO;
import com.ganesh.EV_Project.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/revenue-trends/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<List<DailyStatsDTO>> getRevenueTrends(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "7") int days) {
        
        List<DailyStatsDTO> trends = analyticsService.getRevenueTrends(ownerId, days);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/peak-usage/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<List<PeakHourDTO>> getPeakUsage(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "7") int days) {
        
        List<PeakHourDTO> peakUsage = analyticsService.getPeakUsage(ownerId, days);
        return ResponseEntity.ok(peakUsage);
    }

    @GetMapping("/summary/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<AnalyticsSummaryDTO> getAnalyticsSummary(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "7") int days) {
        
        AnalyticsSummaryDTO summary = analyticsService.getAnalyticsSummary(ownerId, days);
        return ResponseEntity.ok(summary);
    }
}
