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

                log.info("====================================");
                log.info("🎉 ADMIN SEEDING COMPLETED");
                log.info("====================================");
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