package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.EarningsSummaryDTO;
import com.ganesh.EV_Project.service.EarningsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/earnings")
public class EarningsController {

    @Autowired
    private EarningsService earningsService;

    @GetMapping("/summary/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<EarningsSummaryDTO> getEarningsSummary(@PathVariable Long ownerId) {
        return ResponseEntity.ok(earningsService.getEarningsSummary(ownerId));
    }
}
