package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.IoTSensorDataRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

        private final StationRepository stationRepository;
        private final ChargerSlotRepository chargerSlotRepository;
        private final IoTSensorDataRepository ioTSensorDataRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        private final Random random = new Random();

        @Override
        public void run(String... args) {

                log.info("====================================");
                log.info("🚀 EV DATA SEEDING STARTED");
                log.info("====================================");

                seedAdminUser();

                try {
                        List<Station> stations = buildStations();

                        int insertedCount = 0;
                        int skippedCount = 0;

                        for (Station station : stations) {
                                if (stationRepository.existsByName(station.getName())) {
                                        log.warn("⚠️ Already exists: {}", station.getName());
                                        skippedCount++;
                                        continue;
                                }

                                Station savedStation = stationRepository.save(station);
                                createSlotsForStation(savedStation);
                                generateSensorData(savedStation);
                                insertedCount++;
                                log.info("✅ Inserted: {}", savedStation.getName());
                        }

                        log.info("====================================");
                        log.info("🎉 SEEDING COMPLETED");
                        log.info("🆕 Inserted: {}", insertedCount);
                        log.info("⏭️ Skipped (duplicates): {}", skippedCount);
                        log.info("📊 Total Stations in DB: {}", stationRepository.count());
                        log.info("====================================");

                } catch (Exception e) {
                        log.error("❌ ERROR DURING DATA SEEDING", e);
                }
        }

        private List<Station> buildStations() {
                return Arrays.asList(
                                buildStation("Tata Power Ballygunge Charging Hub", "Ballygunge, Kolkata, WB 700019",
                                                22.5230, 88.3650, 4.5, 15.5, true, "24 Hours"),
                                buildStation("ChargeZone Ultadanga EV Station", "Ultadanga, Kolkata, WB 700067",
                                                22.5900, 88.3900, 4.3, 14.5, true, "6 AM - 11 PM"),
                                buildStation("Statiq Shyambazar Charging Point", "Shyambazar, Kolkata, WB 700004",
                                                22.6010, 88.3710, 4.4, 13.5, true, "24 Hours"),
                                buildStation("Zeon Charging Serampore EV Hub", "Serampore, Hooghly, WB 712201", 22.7500,
                                                88.3400, 4.2, 14.0, false, "7 AM - 9 PM"),
                                buildStation("Relux Electric Bankura Station", "Bankura Town, WB 722101", 23.2500,
                                                87.0700, 4.1, 13.0, true, "24 Hours"),
                                buildStation("Ather Grid Mysore Palace Road Hub", "Palace Road, Mysore, KA 570001",
                                                12.3052, 76.6552, 4.6, 17.5, true, "24 Hours"),
                                buildStation("Tata Power Vadodara Alkapuri Station", "Alkapuri, Vadodara, GJ 390007",
                                                22.3072, 73.1812, 4.5, 15.0, true, "6 AM - 11 PM"),
                                buildStation("ChargeZone Raipur GE Road EV Hub", "GE Road, Raipur, CG 492001", 21.2514,
                                                81.6296, 4.3, 14.5, true, "24 Hours"),
                                buildStation("Fortum EV Shillong Police Bazar", "Police Bazar, Shillong, ML 793001",
                                                25.5788, 91.8933, 4.4, 15.0, true, "7 AM - 11 PM"),
                                buildStation("Statiq Amritsar Golden Temple Hub",
                                                "Near Golden Temple, Amritsar, PB 143006", 31.6200, 74.8765, 4.7, 18.0,
                                                true, "24 Hours"));
        }

        private Station buildStation(String name, String address, double lat, double lng, double rating, double price,
                        boolean isOpen, String hours) {
                return Station.builder()
                                .name(name)
                                .address(address)
                                .latitude(lat)
                                .longitude(lng)
                                .meta(generateMeta())
                                .rating(rating)
                                .operatingHours(hours)
                                .pricePerKwh(price)
                                .isOpen(isOpen)
                                .lastUsedTime(generateLastUsedTime())
                                .build();
        }

        private String generateMeta() {
                List<String> features = Arrays.asList("Fast Charging Available", "CCTV Surveillance", "24x7 Security",
                                "Parking Available", "Near Highway", "Cafe Nearby", "Restroom Available",
                                "Shopping Complex", "Solar Powered");
                Collections.shuffle(features);
                return String.join(", ", features.subList(0, 3));
        }

        private LocalDateTime generateLastUsedTime() {
                return LocalDateTime.now().minusMinutes(random.nextInt(180));
        }

        private void createSlotsForStation(Station station) {
                int slotCount = 2 + random.nextInt(4);
                List<ChargerSlot> slots = new ArrayList<>();
                for (int i = 1; i <= slotCount; i++) {
                        ConnectorType connector = getRandomConnector();
                        SlotType slotType = (connector == ConnectorType.TYPE_2) ? SlotType.AC : SlotType.DC;
                        SlotStatus status = random.nextDouble() > 0.7 ? SlotStatus.OCCUPIED : SlotStatus.AVAILABLE;
                        double power = (slotType == SlotType.DC) ? (50 + random.nextInt(100))
                                        : (7 + random.nextInt(15));
                        ChargerSlot slot = ChargerSlot.builder().station(station).slotLabel("SLOT-" + i)
                                        .connectorType(connector).slotType(slotType).status(status).powerKw(power)
                                        .build();
                        slots.add(slot);
                }
                chargerSlotRepository.saveAll(slots);
        }

        private ConnectorType getRandomConnector() {
                ConnectorType[] types = { ConnectorType.CCS2, ConnectorType.TYPE_2, ConnectorType.GB_T };
                return types[random.nextInt(types.length)];
        }

        private void generateSensorData(Station station) {
                LocalDateTime timestamp = LocalDateTime.now().minusMinutes(random.nextInt(60));
                var data = com.ganesh.EV_Project.model.IoTSensorData.builder().station(station)
                                .voltage(220 + random.nextDouble() * 20).current(10 + random.nextDouble() * 20)
                                .power(5 + random.nextDouble() * 40).timestamp(timestamp).build();
                station.setLastUsedTime(timestamp);
                stationRepository.save(station);
                ioTSensorDataRepository.save(data);
        }

        private void seedAdminUser() {
                String adminEmail = "admin@ev.com";
                if (userRepository.findByEmail(adminEmail).isEmpty()) {
                        User admin = new User();
                        admin.setName("System Admin");
                        admin.setEmail(adminEmail);
                        admin.setPassword(passwordEncoder.encode("admin123"));
                        admin.setRole(User.Role.ADMIN);
                        admin.setIsFirstTimeUser(false);
                        userRepository.save(admin);
                        log.info("👤 Default Admin User created: {}", adminEmail);
                } else {
                        log.info("ℹ️ Admin user already exists. Skipping admin seeding.");
                }
        }
}