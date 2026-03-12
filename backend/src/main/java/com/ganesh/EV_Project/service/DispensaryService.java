package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.repository.DispensaryRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DispensaryService {

    @Autowired
    private DispensaryRepository dispensaryRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerSlotRepository chargerSlotRepository;

    public List<Dispensary> getByStation(Long stationId) {
        return dispensaryRepository.findByStationIdOrderByIdAsc(stationId);
    }

    @Transactional
    public Dispensary addToStation(Long stationId, Dispensary dispensary) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new APIException("Station not found"));
        dispensary.setStation(station);
        // Ensure a default connector type if not provided
        if (dispensary.getConnectorType() == null) {
            dispensary.setConnectorType(ConnectorType.CCS2);
        }
        Dispensary saved = dispensaryRepository.save(dispensary);

        // Create guns based on numberOfGuns (1 or 2)
        int gunCount = saved.getNumberOfGuns() != null ? saved.getNumberOfGuns() : 2;
        double powerPerGun = saved.getTotalPowerKw() != null ? saved.getTotalPowerKw() / gunCount : 30.0;
        ConnectorType connectorType = saved.getConnectorType();
        // Derive SlotType from connector: CCS2 → DC, TYPE_2 → AC
        SlotType slotType = (connectorType == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;
        for (int i = 1; i <= gunCount; i++) {
            ChargerSlot slot = ChargerSlot.builder()
                    .station(station)
                    .dispensary(saved)
                    .slotLabel(saved.getName() + " - Gun " + i)
                    .slotType(slotType)
                    .status(SlotStatus.AVAILABLE)
                    .connectorType(connectorType)
                    .powerKw(powerPerGun)
                    .build();
            chargerSlotRepository.save(slot);
        }

        return saved;
    }

    @Transactional
    public Dispensary updateDispensary(Long id, Dispensary updated) {
        Dispensary existing = dispensaryRepository.findById(id)
                .orElseThrow(() -> new APIException("Dispensary not found"));

        existing.setName(updated.getName());
        existing.setTotalPowerKw(updated.getTotalPowerKw());
        existing.setAcceptsTrucks(updated.getAcceptsTrucks());

        // Update power on all child guns (each gun gets half the dispenser total)
        List<ChargerSlot> guns = chargerSlotRepository.findByDispensary(existing);
        for (ChargerSlot gun : guns) {
            gun.setPowerKw(updated.getTotalPowerKw() / 2.0);
            chargerSlotRepository.save(gun);
        }

        return dispensaryRepository.save(existing);
    }

    @Transactional
    public void updateDispensaryConnectorType(Long dispensaryId, ConnectorType connectorType) {
        Dispensary dispensary = dispensaryRepository.findById(dispensaryId)
                .orElseThrow(() -> new APIException("Dispensary not found"));
        // Update source of truth on dispensary
        dispensary.setConnectorType(connectorType);
        dispensaryRepository.save(dispensary);
        // Also sync connector type and slotType on each gun
        SlotType slotType = (connectorType == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;
        List<ChargerSlot> guns = chargerSlotRepository.findByDispensary(dispensary);
        for (ChargerSlot gun : guns) {
            gun.setConnectorType(connectorType);
            gun.setSlotType(slotType);
            chargerSlotRepository.save(gun);
        }
    }

    @Transactional
    public void deleteDispensary(Long id) {
        Dispensary dispensary = dispensaryRepository.findById(id)
                .orElseThrow(() -> new APIException("Dispensary not found"));
        // Delete child slots first
        List<ChargerSlot> guns = chargerSlotRepository.findByDispensary(dispensary);
        chargerSlotRepository.deleteAll(guns);
        dispensaryRepository.delete(dispensary);
    }
}
