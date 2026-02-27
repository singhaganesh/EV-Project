package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Station getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new APIException("Station not found"));
        return station;
    }

    public Station addStation(Station station) {
        return stationRepository.save(station);
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

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
