package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

        @Autowired
        private StationRepository stationRepository;

        @Autowired
        private com.ganesh.EV_Project.repository.IoTSensorDataRepository ioTSensorDataRepository;

        @Autowired
        private ChargerSlotRepository chargerSlotRepository;

        @Override
        public void run(String... args) throws Exception {
                System.out.println("Data Seeding: proper seeding started...");

                List<Station> stationsToSeed = Arrays.asList(
                                Station.builder()
                                                .name("ChargePoint Mumbai Central")
                                                .address("Mumbai Central, Mumbai, Maharashtra 400008")
                                                .latitude(19.0760).longitude(72.8777)
                                                .meta("Fast charging available")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("EcoCharge Bandra")
                                                .address("Bandra West, Mumbai, Maharashtra 400050")
                                                .latitude(19.0596).longitude(72.8295)
                                                .meta("Shopping mall parking")
                                                .rating(4.5)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(18.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Vashi EV Hub")
                                                .address("Sector 17, Vashi, Navi Mumbai, Maharashtra 400703")
                                                .latitude(19.0330).longitude(73.0297)
                                                .meta("24/7 Open")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("GreenPower Thane")
                                                .address("Ghodbunder Road, Thane, Maharashtra 400607")
                                                .latitude(19.2183).longitude(72.9781)
                                                .meta("Highway access")
                                                .rating(4.0)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(16.0).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Powai Lake Chargers")
                                                .address("Powai, Mumbai, Maharashtra 400076")
                                                .latitude(19.1176).longitude(72.9060)
                                                .meta("Scenic view while charging")
                                                .rating(4.8)
                                                .operatingHours("7 AM - 11 PM").pricePerKwh(17.5).isOpen(true)
                                                .build(),
                                // Kolkata Stations
                                Station.builder()
                                                .name("EcoCharge Kolkata Center")
                                                .address("Park Street, Kolkata, West Bengal 700016")
                                                .latitude(22.5551).longitude(88.3514)
                                                .meta("Central business district")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(14.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Salt Lake Sector V Hub")
                                                .address("Sector V, Salt Lake, Kolkata, West Bengal 700091")
                                                .latitude(22.5726).longitude(88.4374)
                                                .meta("IT Park area")
                                                .rating(4.4)
                                                .operatingHours("8 AM - 10 PM").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("New Town Green Energy")
                                                .address("New Town, Kolkata, West Bengal 700156")
                                                .latitude(22.5866).longitude(88.4616)
                                                .meta("Near Eco Park")
                                                .rating(4.3)
                                                .operatingHours("24 Hours").pricePerKwh(13.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("South City Mall Chargers")
                                                .address("Prince Anwar Shah Rd, Kolkata, West Bengal 700068")
                                                .latitude(22.5020).longitude(88.3615)
                                                .meta("Mall basement parking")
                                                .rating(4.7)
                                                .operatingHours("10 AM - 9 PM").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Gariahat EV Point")
                                                .address("Ballygunge Gardens, Gariahat, Kolkata, West Bengal 700019")
                                                .latitude(22.5180).longitude(88.3643)
                                                .meta("Market area parking")
                                                .rating(4.1)
                                                .operatingHours("7 AM - 10 PM").pricePerKwh(16.5).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Esplanade Metro Chargers")
                                                .address("Jawaharlal Nehru Road, Esplanade, Kolkata, West Bengal 700013")
                                                .latitude(22.5646).longitude(88.3517)
                                                .meta("Metro station access")
                                                .rating(4.5)
                                                .operatingHours("5 AM - 12 AM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Science City Energy Hub")
                                                .address("J.B.S Haldane Avenue, Kolkata, West Bengal 700046")
                                                .latitude(22.5392).longitude(88.3963)
                                                .meta("Convention center parking")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(12.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Dum Dum Airport Zone")
                                                .address("Jessore Road, Dum Dum, Kolkata, West Bengal 700028")
                                                .latitude(22.6420).longitude(88.4311)
                                                .meta("Airport approach road")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Victoria Green Spot")
                                                .address("Queens Way, Maidan, Kolkata, West Bengal 700071")
                                                .latitude(22.5448).longitude(88.3426)
                                                .meta("Tourist spot parking")
                                                .rating(4.9)
                                                .operatingHours("6 AM - 9 PM").pricePerKwh(16.5).isOpen(true)
                                                .build());

                int stationIndex = 0;
                for (Station station : stationsToSeed) {
                        if (!stationRepository.existsByName(station.getName())) {
                                Station savedStation = stationRepository.save(station);
                                createSlotsForStation(savedStation, stationIndex);
                                generateSensorDataForStation(savedStation);
                                System.out.println("Seeded station: " + station.getName());
                        } else {
                                System.out.println("Station already exists: " + station.getName());
                        }
                        stationIndex++;
                }

                System.out.println("Data Seeding: Completed.");
        }

        private void createSlotsForStation(Station station, int stationIndex) {
                // Vary the slot configuration per station for realistic data
                switch (stationIndex % 5) {
                        case 0: // 3 DC fast chargers
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.CHARGING).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build()));
                                break;
                        case 1: // 2 AC + 2 DC mix
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(30.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.OCCUPIED).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build()));
                                break;
                        case 2: // 4 AC slow chargers
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.MAINTENANCE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(22.0).build()));
                                break;
                        case 3: // 2 Tesla superchargers + 1 CCS2
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.TESLA)
                                                                .status(SlotStatus.AVAILABLE).powerKw(120.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.TESLA)
                                                                .status(SlotStatus.CHARGING).powerKw(120.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(60.0).build()));
                                break;
                        case 4: // 5 varied connectors
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.OCCUPIED).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("C1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.GB_T)
                                                                .status(SlotStatus.MAINTENANCE).powerKw(60.0).build()));
                                break;
                }
        }

        private void generateSensorDataForStation(Station station) {
                int minutesAgo = (int) (Math.random() * 120);
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.now().minusMinutes(minutesAgo);

                com.ganesh.EV_Project.model.IoTSensorData data = com.ganesh.EV_Project.model.IoTSensorData.builder()
                                .station(station)
                                .voltage(230.0 + (Math.random() * 10))
                                .current(15.0 + (Math.random() * 15))
                                .power(3.5 + (Math.random() * 3.5))
                                .timestamp(timestamp)
                                .build();

                station.setLastUsedTime(timestamp);
                stationRepository.save(station);
                ioTSensorDataRepository.save(data);
        }
}
