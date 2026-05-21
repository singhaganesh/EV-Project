package com.ganesh.finder.config;

import com.ganesh.finder.repository.StationRepository;
import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StationImportService stationImportService;
    private final StationRepository stationRepository;

    public DataSeeder(StationImportService stationImportService, StationRepository stationRepository) {
        this.stationImportService = stationImportService;
        this.stationRepository = stationRepository;
    }

    @Override
    public void run(String... args) {
        long currentCount = stationRepository.count();
        if (currentCount > 0) {
            log.info("====================================");
            log.info("🔍 DataSeeder: Database already contains {} stations. Skipping seeding.", currentCount);
            log.info("====================================");
            return;
        }

        log.info("====================================");
        log.info("🔍 STATION FINDER DATA SEEDER STARTED");
        log.info("====================================");

        try {
            // Import stations for major Indian cities on first run
            // Using radius 50km for each city center

            log.info("🗺️ Importing stations from Mumbai...");
            int mumbaiCount = stationImportService.importFromOCM(19.0760, 72.8777, 50);
            log.info("✅ Imported {} stations from Mumbai region", mumbaiCount);

            log.info("🗺️ Importing stations from Bangalore...");
            int bangaloreCount = stationImportService.importFromOCM(12.9716, 77.5946, 50);
            log.info("✅ Imported {} stations from Bangalore region", bangaloreCount);

            log.info("🗺️ Importing stations from Delhi...");
            int delhiCount = stationImportService.importFromOCM(28.7041, 77.1025, 50);
            log.info("✅ Imported {} stations from Delhi region", delhiCount);

            log.info("🗺️ Importing stations from Hyderabad...");
            int hyderabadCount = stationImportService.importFromOCM(17.3850, 78.4867, 50);
            log.info("✅ Imported {} stations from Hyderabad region", hyderabadCount);

            log.info("🗺️ Importing stations from Chennai...");
            int chennaiCount = stationImportService.importFromOCM(13.0827, 80.2707, 50);
            log.info("✅ Imported {} stations from Chennai region", chennaiCount);

            int total = mumbaiCount + bangaloreCount + delhiCount + hyderabadCount + chennaiCount;
            log.info("====================================");
            log.info("🎉 SEEDING COMPLETE: {} total stations imported", total);
            log.info("====================================");

        } catch (Exception e) {
            log.warn("⚠️ Seeding skipped or partially completed: {}", e.getMessage());
        }
    }
}
