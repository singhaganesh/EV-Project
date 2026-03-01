package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargerSlotRepository chargerSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Station> getStationsByOwnerId(Long ownerId) {
        return stationRepository.findByOwnerId(ownerId);
    }

    public Station getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new APIException("Station not found"));
        return station;
    }

    public Station addStation(Station station) {
        if (station.getDispensaries() != null) {
            for (Dispensary d : station.getDispensaries()) {
                d.setStation(station);
            }
        }

        Station savedStation = stationRepository.save(station);

        if (savedStation.getDispensaries() != null) {
            for (Dispensary d : savedStation.getDispensaries()) {
                double powerPerGun = d.getTotalPowerKw() != null ? d.getTotalPowerKw() / 2.0 : 30.0;

                for (int i = 1; i <= 2; i++) {
                    com.ganesh.EV_Project.model.ChargerSlot slot = com.ganesh.EV_Project.model.ChargerSlot.builder()
                            .station(savedStation)
                            .dispensary(d)
                            .slotLabel(d.getName() + " - Gun " + i)
                            .slotType(com.ganesh.EV_Project.enums.SlotType.DC) // Defaulting to DC
                            .status(com.ganesh.EV_Project.enums.SlotStatus.AVAILABLE)
                            .connectorType(com.ganesh.EV_Project.enums.ConnectorType.CCS2) // Defaulting to CCS2
                            .powerKw(powerPerGun)
                            .build();
                    chargerSlotRepository.save(slot);
                }
            }
        }

        return savedStation;
    }

    public Station updateStation(Long id, Station updatedStation) {
        return stationRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedStation.getName());
                    existing.setLatitude(updatedStation.getLatitude());
                    existing.setLongitude(updatedStation.getLongitude());
                    existing.setAddress(updatedStation.getAddress());
                    existing.setMeta(updatedStation.getMeta());
                    return stationRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Station not found"));
    }

    @Transactional
    public void deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new APIException("Station not found"));

        List<com.ganesh.EV_Project.model.ChargerSlot> slots = chargerSlotRepository.findByStation(station);
        for (com.ganesh.EV_Project.model.ChargerSlot slot : slots) {
            bookingRepository.deleteBySlot(slot);
        }

        chargerSlotRepository.deleteByStation(station);
        stationRepository.delete(station);
    }
}
