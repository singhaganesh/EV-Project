package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.DispensaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispensaries")
public class DispensaryController {

    @Autowired
    private DispensaryService dispensaryService;

    @GetMapping("/station/{stationId}")
    public ResponseEntity<APIResponse> getByStation(@PathVariable Long stationId) {
        List<Dispensary> dispensaries = dispensaryService.getByStation(stationId);
        return ResponseEntity.ok(new APIResponse(true, "Dispensaries fetched", dispensaries));
    }

    @PostMapping("/station/{stationId}")
    public ResponseEntity<APIResponse> addToStation(
            @PathVariable Long stationId,
            @RequestBody Dispensary dispensary) {
        Dispensary saved = dispensaryService.addToStation(stationId, dispensary);
        return new ResponseEntity<>(new APIResponse(true, "Dispensary added with 2 guns", saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse> updateDispensary(
            @PathVariable Long id,
            @RequestBody Dispensary dispensary) {
        Dispensary updated = dispensaryService.updateDispensary(id, dispensary);
        return ResponseEntity.ok(new APIResponse(true, "Dispensary updated", updated));
    }

    @PutMapping("/{id}/connectorType")
    public ResponseEntity<APIResponse> updateDispensaryConnectorType(
            @PathVariable Long id,
            @RequestParam ConnectorType connectorType) {
        dispensaryService.updateDispensaryConnectorType(id, connectorType);
        return ResponseEntity.ok(new APIResponse(true, "Connector type updated for all guns", null));
    }

    @GetMapping("/connector-types")
    public ResponseEntity<ConnectorType[]> getConnectorTypes() {
        return ResponseEntity.ok(ConnectorType.values());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> deleteDispensary(@PathVariable Long id) {
        dispensaryService.deleteDispensary(id);
        return ResponseEntity.ok(new APIResponse(true, "Dispensary and its guns deleted", null));
    }
}
