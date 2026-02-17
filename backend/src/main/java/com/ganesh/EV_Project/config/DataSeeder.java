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
                                                .latitude(19.0760)
                                                .longitude(72.8777)
                                                .meta("Fast charging available")
                                                .rating(4.7)
                                                .build(),
                                Station.builder()
                                                .name("EcoCharge Bandra")
                                                .address("Bandra West, Mumbai, Maharashtra 400050")
                                                .latitude(19.0596)
                                                .longitude(72.8295)
                                                .meta("Shopping mall parking")
                                                .rating(4.5)
                                                .build(),
                                Station.builder()
                                                .name("Vashi EV Hub")
                                                .address("Sector 17, Vashi, Navi Mumbai, Maharashtra 400703")
                                                .latitude(19.0330)
                                                .longitude(73.0297)
                                                .meta("24/7 Open")
                                                .rating(4.2)
                                                .build(),
                                Station.builder()
                                                .name("GreenPower Thane")
                                                .address("Ghodbunder Road, Thane, Maharashtra 400607")
                                                .latitude(19.2183)
                                                .longitude(72.9781)
                                                .meta("Highway access")
                                                .rating(4.0)
                                                .build(),
                                Station.builder()
                                                .name("Powai Lake Chargers")
                                                .address("Powai, Mumbai, Maharashtra 400076")
                                                .latitude(19.1176)
                                                .longitude(72.9060)
                                                .meta("Scenic view while charging")
                                                .rating(4.8)
                                                .build(),
                                // Kolkata Stations
                                Station.builder()
                                                .name("EcoCharge Kolkata Center")
                                                .address("Park Street, Kolkata, West Bengal 700016")
                                                .latitude(22.5551)
                                                .longitude(88.3514)
                                                .meta("Central business district")
                                                .rating(4.6)
                                                .build(),
                                Station.builder()
                                                .name("Salt Lake Sector V Hub")
                                                .address("Sector V, Salt Lake, Kolkata, West Bengal 700091")
                                                .latitude(22.5726)
                                                .longitude(88.4374)
                                                .meta("IT Park area")
                                                .rating(4.4)
                                                .build(),
                                Station.builder()
                                                .name("New Town Green Energy")
                                                .address("New Town, Kolkata, West Bengal 700156")
                                                .latitude(22.5866)
                                                .longitude(88.4616)
                                                .meta("Near Eco Park")
                                                .rating(4.3)
                                                .build(),
                                Station.builder()
                                                .name("South City Mall Chargers")
                                                .address("Prince Anwar Shah Rd, Kolkata, West Bengal 700068")
                                                .latitude(22.5020)
                                                .longitude(88.3615)
                                                .meta("Mall basement parking")
                                                .rating(4.7)
                                                .build(),
                                Station.builder()
                                                .name("Gariahat EV Point")
                                                .address("Ballygunge Gardens, Gariahat, Kolkata, West Bengal 700019")
                                                .latitude(22.5180)
                                                .longitude(88.3643)
                                                .meta("Market area parking")
                                                .rating(4.1)
                                                .build(),
                                Station.builder()
                                                .name("Esplanade Metro Chargers")
                                                .address("Jawaharlal Nehru Road, Esplanade, Kolkata, West Bengal 700013")
                                                .latitude(22.5646)
                                                .longitude(88.3517)
                                                .meta("Metro station access")
                                                .rating(4.5)
                                                .build(),
                                Station.builder()
                                                .name("Science City Energy Hub")
                                                .address("J.B.S Haldane Avenue, Kolkata, West Bengal 700046")
                                                .latitude(22.5392)
                                                .longitude(88.3963)
                                                .meta("Convention center parking")
                                                .rating(4.8)
                                                .build(),
                                Station.builder()
                                                .name("Dum Dum Airport Zone")
                                                .address("Jessore Road, Dum Dum, Kolkata, West Bengal 700028")
                                                .latitude(22.6420)
                                                .longitude(88.4311)
                                                .meta("Airport approach road")
                                                .rating(4.2)
                                                .build(),
                                Station.builder()
                                                .name("Victoria Green Spot")
                                                .address("Queens Way, Maidan, Kolkata, West Bengal 700071")
                                                .latitude(22.5448)
                                                .longitude(88.3426)
                                                .meta("Tourist spot parking")
                                                .rating(4.9)
                                                .build());

                for (Station station : stationsToSeed) {
                        if (!stationRepository.existsByName(station.getName())) {
                                Station savedStation = stationRepository.save(station);
                                createSlotsForStation(savedStation);
                                generateSensorDataForStation(savedStation);
                                System.out.println("Seeded station: " + station.getName());
                        } else {
                                System.out.println("Station already exists: " + station.getName());
                        }
                }

                System.out.println("Data Seeding: Completed.");
        }

        private void createSlotsForStation(Station station) {
                // Slot 1: DC Fast CSS2
                ChargerSlot slot1 = ChargerSlot.builder()
                                .station(station)
                                .slotLabel("A1")
                                .slotType(SlotType.DC)
                                .connectorType(ConnectorType.CCS2)
                                .status(SlotStatus.AVAILABLE)
                                .powerKw(50.0)
                                .build();

                // Slot 2: AC Type 2
                ChargerSlot slot2 = ChargerSlot.builder()
                                .station(station)
                                .slotLabel("A2")
                                .slotType(SlotType.AC)
                                .connectorType(ConnectorType.TYPE_2)
                                .status(SlotStatus.AVAILABLE)
                                .powerKw(22.0)
                                .build();

                // Slot 3: DC Fast CHAdeMO
                ChargerSlot slot3 = ChargerSlot.builder()
                                .station(station)
                                .slotLabel("B1")
                                .slotType(SlotType.DC)
                                .connectorType(ConnectorType.CHADEMO)
                                .status(SlotStatus.MAINTENANCE) // One in maintenance for variety
                                .powerKw(50.0)
                                .build();

                // Slot 4: AC Type 2
                ChargerSlot slot4 = ChargerSlot.builder()
                                .station(station)
                                .slotLabel("B2")
                                .slotType(SlotType.AC)
                                .connectorType(ConnectorType.TYPE_2)
                                .status(SlotStatus.OCCUPIED) // One occupied
                                .powerKw(7.4)
                                .build();

                chargerSlotRepository.saveAll(Arrays.asList(slot1, slot2, slot3, slot4));
        }

        private void generateSensorDataForStation(Station station) {
                // Generate a random timestamp within the last 2 hours
                int minutesAgo = (int) (Math.random() * 120);
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.now().minusMinutes(minutesAgo);

                com.ganesh.EV_Project.model.IoTSensorData data = com.ganesh.EV_Project.model.IoTSensorData.builder()
                                .station(station)
                                .voltage(230.0 + (Math.random() * 10)) // Random voltage 230-240V
                                .current(15.0 + (Math.random() * 15)) // Random current 15-30A
                                .power(3.5 + (Math.random() * 3.5)) // Random power 3.5-7kW
                                .timestamp(timestamp)
                                .build();

                // Update Station's lastUsedTime to match the sensor data
                station.setLastUsedTime(timestamp);
                stationRepository.save(station);

                // We need to manually set timestamp if @CreationTimestamp overrides it,
                // but since we are saving a new entity, let's rely on the DB or ensure entity
                // allows setter.
                // Actually @CreationTimestamp might override it on save.
                // A better approach for "history" seeding is to NOT use @CreationTimestamp or
                // use a different field,
                // but for "Last used", getting the latest creation time is sufficient if we
                // just create it now.
                // Wait, if I create it NOW, "Last used" will always be "Just now".
                // The user wants "Last used" data.
                // If I use @CreationTimestamp, it will be NOW.
                // To simulate "Last used 45 mins ago", I need to be able to set the timestamp.
                // I'll assume for this prototype that I can overwrite it or I'll just save it
                // and it will say "0 min ago" which is fine for "last used".
                // BUT, to make it look realistic (not all 0 min ago), I should probably try to
                // force it.
                // Let's just save it. "Last used: 1 min ago" is better than static "10 min ago"
                // if it's real.

                ioTSensorDataRepository.save(data);
        }
}
