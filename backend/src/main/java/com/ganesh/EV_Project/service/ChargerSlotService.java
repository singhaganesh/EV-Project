package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargerSlotService {

    @Autowired
    private ChargerSlotRepository slotRepository;
    @Autowired
    private StationRepository stationRepository;


    public List<ChargerSlot> getAllSlots() {
        return slotRepository.findAll();
    }

    public List<ChargerSlot> getSlotsByStation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        return slotRepository.findByStation(station);
    }

    public List<ChargerSlot> getAvailableSlots(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        return slotRepository.findByStationAndStatus(station,SlotStatus.AVAILABLE);
    }

    public ChargerSlot updateSlotStatus(Long id, SlotStatus status) {
        ChargerSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new APIException("Slot not found"));
        slot.setStatus(status);
        return slotRepository.save(slot);
    }

    public ChargerSlot createSlot(ChargerSlot slot, Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        slot.setStation(station);
        slot.setStatus(SlotStatus.AVAILABLE);
        return slotRepository.save(slot);
    }
}

