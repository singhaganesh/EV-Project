package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Station addStation(Station station) {
        System.out.println("Adding station: " + station.getName());
        if (station.getDispensaries() != null) {
            System.out.println("Contains " + station.getDispensaries().size() + " dispensaries");
            for (Dispensary d : station.getDispensaries()) {
                d.setStation(station);
                // Ensure connectorType has a value before persisting 
                if (d.getConnectorType() == null) {
                    d.setConnectorType(ConnectorType.CCS2);
                }
            }
        }

        Station savedStation = stationRepository.save(station);

        if (savedStation.getDispensaries() != null) {
            for (Dispensary d : savedStation.getDispensaries()) {
                int gunCount = d.getNumberOfGuns() != null ? d.getNumberOfGuns() : 2;
                double powerPerGun = d.getTotalPowerKw() != null ? d.getTotalPowerKw() / gunCount : 30.0;
                ConnectorType ct = d.getConnectorType() != null ? d.getConnectorType() : ConnectorType.CCS2;
                // Derive SlotType from connector: CCS2 → DC, TYPE_2 → AC
                SlotType slotType = (ct == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;

                for (int i = 1; i <= gunCount; i++) {
                    com.ganesh.EV_Project.model.ChargerSlot slot = com.ganesh.EV_Project.model.ChargerSlot.builder()
                            .station(savedStation)
                            .dispensary(d)
                            .slotLabel(d.getName() + " - Gun " + i)
                            .slotType(slotType)
                            .status(SlotStatus.AVAILABLE)
                            .connectorType(ct)
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
                    existing.setOperatingHours(updatedStation.getOperatingHours());
                    existing.setPricePerKwh(updatedStation.getPricePerKwh());
                    existing.setTruckPricePerKwh(updatedStation.getTruckPricePerKwh());
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
