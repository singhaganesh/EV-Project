package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.DispensaryService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispensaries")
public class DispensaryController {

    @Autowired
    private DispensaryService dispensaryService;

    @Autowired
    private UserService userService;

    private ResponseEntity<APIResponse> forbidden() {
        return new ResponseEntity<>(
                new APIResponse("Access denied: not your station", false), HttpStatus.FORBIDDEN);
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<APIResponse> getByStation(@PathVariable Long stationId) {
        List<Dispensary> dispensaries = dispensaryService.getByStation(stationId);
        return ResponseEntity.ok(new APIResponse(true, "Dispensaries fetched", dispensaries));
    }

    @PostMapping("/station/{stationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<APIResponse> addToStation(
            @PathVariable Long stationId,
            @RequestBody Dispensary dispensary,
            Authentication authentication) {
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (!dispensaryService.isStationOwnedBy(stationId, currentUser)) {
            return forbidden();
        }
        Dispensary saved = dispensaryService.addToStation(stationId, dispensary);
        return new ResponseEntity<>(new APIResponse(true, "Dispensary added with 2 guns", saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<APIResponse> updateDispensary(
            @PathVariable Long id,
            @RequestBody Dispensary dispensary,
            Authentication authentication) {
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (!dispensaryService.isDispensaryOwnedBy(id, currentUser)) {
            return forbidden();
        }
        Dispensary updated = dispensaryService.updateDispensary(id, dispensary);
        return ResponseEntity.ok(new APIResponse(true, "Dispensary updated", updated));
    }

    @PutMapping("/{id}/connectorType")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<APIResponse> updateDispensaryConnectorType(
            @PathVariable Long id,
            @RequestParam ConnectorType connectorType,
            Authentication authentication) {
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (!dispensaryService.isDispensaryOwnedBy(id, currentUser)) {
            return forbidden();
        }
        dispensaryService.updateDispensaryConnectorType(id, connectorType);
        return ResponseEntity.ok(new APIResponse(true, "Connector type updated for all guns", null));
    }

    @GetMapping("/connector-types")
    public ResponseEntity<ConnectorType[]> getConnectorTypes() {
        return ResponseEntity.ok(ConnectorType.values());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STATION_OWNER')")
    public ResponseEntity<APIResponse> deleteDispensary(@PathVariable Long id,
                                                        Authentication authentication) {
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (!dispensaryService.isDispensaryOwnedBy(id, currentUser)) {
            return forbidden();
        }
        dispensaryService.deleteDispensary(id);
        return ResponseEntity.ok(new APIResponse(true, "Dispensary and its guns deleted", null));
    }
}
