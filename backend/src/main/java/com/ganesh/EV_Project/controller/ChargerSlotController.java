package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.service.ChargerSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class ChargerSlotController {

    @Autowired
    private ChargerSlotService slotService;

    @GetMapping
    public ResponseEntity<List<ChargerSlot>> getAllSlots() {
        List<ChargerSlot> chargerSlots = slotService.getAllSlots();
        return new ResponseEntity<>(chargerSlots, HttpStatus.OK);
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<com.ganesh.EV_Project.payload.APIResponse> getSlotsByStation(@PathVariable Long stationId) {
        List<ChargerSlot> slotsByStation = slotService.getSlotsByStation(stationId);
        return new ResponseEntity<>(
                new com.ganesh.EV_Project.payload.APIResponse(true, "Slots fetched successfully", slotsByStation),
                HttpStatus.OK);
    }

    @GetMapping("/station/{stationId}/available")
    public ResponseEntity<com.ganesh.EV_Project.payload.APIResponse> getAvailableSlots(@PathVariable Long stationId) {
        System.out.println("First");
        List<ChargerSlot> availableSlots = slotService.getAvailableSlots(stationId);
        System.out.println("Second");
        return new ResponseEntity<>(
                new com.ganesh.EV_Project.payload.APIResponse(true, "Available slots fetched", availableSlots),
                HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ChargerSlot> updateSlotStatus(@PathVariable Long id, @RequestParam SlotStatus status) {
        ChargerSlot updatedSlot = slotService.updateSlotStatus(id, status);
        return new ResponseEntity<>(updatedSlot, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargerSlot> getSlotById(@PathVariable Long id) {
        ChargerSlot slot = slotService.getSlotById(id);
        return new ResponseEntity<>(slot, HttpStatus.OK);
    }

    @PostMapping("/station/{stationId}")
    public ResponseEntity<ChargerSlot> createSlot(@RequestBody ChargerSlot slot, @PathVariable Long stationId) {
        ChargerSlot savedSlot = slotService.createSlot(slot, stationId);
        return new ResponseEntity<>(savedSlot, HttpStatus.CREATED);
    }
}
