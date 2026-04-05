package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.DailyStatsDTO;
import com.ganesh.EV_Project.dto.PeakHourDTO;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private ChargingSessionRepository sessionRepository;

    public List<DailyStatsDTO> getRevenueTrends(Long ownerId, int days) {
        // ... (existing code unchanged)
        LocalDateTime since = LocalDateTime.now().minusDays(days).with(LocalTime.MIN);
        List<Object[]> rawData = sessionRepository.getDailyStatsByOwner(ownerId, since);

        // Convert raw results to a map for easy lookup
        Map<LocalDate, DailyStatsDTO> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> {
                            if (row[0] instanceof java.sql.Date) {
                                return ((java.sql.Date) row[0]).toLocalDate();
                            } else if (row[0] instanceof java.time.LocalDate) {
                                return (java.time.LocalDate) row[0];
                            }
                            return LocalDate.parse(row[0].toString());
                        },
                        row -> new DailyStatsDTO(
                                row[0] instanceof java.sql.Date ? ((java.sql.Date) row[0]).toLocalDate() : (LocalDate) row[0],
                                ((Number) row[1]).doubleValue(),
                                ((Number) row[2]).doubleValue()
                        ),
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));

        // Fill gaps with 0 values
        List<DailyStatsDTO> filledData = new ArrayList<>();
        LocalDate startDate = since.toLocalDate();
        LocalDate endDate = LocalDate.now();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (dataMap.containsKey(date)) {
                filledData.add(dataMap.get(date));
            } else {
                filledData.add(new DailyStatsDTO(date, 0.0, 0.0));
            }
        }

        return filledData;
    }

    public List<PeakHourDTO> getPeakUsage(Long ownerId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days).with(LocalTime.MIN);
        List<Object[]> rawData = sessionRepository.getPeakUsageByOwner(ownerId, since);

        Map<Integer, Long> hourMap = new HashMap<>();
        for (Object[] row : rawData) {
            hourMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        List<PeakHourDTO> filledData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            filledData.add(new PeakHourDTO(i, hourMap.getOrDefault(i, 0L)));
        }

        return filledData;
    }
}
