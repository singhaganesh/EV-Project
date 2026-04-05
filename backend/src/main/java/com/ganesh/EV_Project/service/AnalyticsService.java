package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.*;
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

    public AnalyticsSummaryDTO getAnalyticsSummary(Long ownerId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days).with(LocalTime.MIN);

        // 1. Fetch Efficiency Metrics with null-safety
        List<Object[]> efficiencyRawList = sessionRepository.getEfficiencyMetrics(ownerId, since);
        EfficiencyMetricsDTO efficiency;
        
        if (efficiencyRawList != null && !efficiencyRawList.isEmpty()) {
            Object[] row = efficiencyRawList.get(0);
            efficiency = new EfficiencyMetricsDTO(
                    row[0] != null ? ((Number) row[0]).doubleValue() : 0.0,
                    row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                    row[2] != null ? ((Number) row[2]).doubleValue() : 0.0
            );
        } else {
            efficiency = new EfficiencyMetricsDTO(0.0, 0.0, 0.0);
        }

        // 2. Fetch Station Revenue with null-safety
        List<Object[]> stationRaw = sessionRepository.getRevenueByStation(ownerId, since);
        List<StationRevenueDTO> stationRevenue = new ArrayList<>();
        if (stationRaw != null) {
            stationRevenue = stationRaw.stream()
                    .map(row -> new StationRevenueDTO(
                            row[0] != null ? row[0].toString() : "Unknown", 
                            row[1] != null ? ((Number) row[1]).doubleValue() : 0.0
                    ))
                    .collect(Collectors.toList());
        }

        // 3. Fetch Connector Revenue with null-safety
        List<Object[]> connectorRaw = sessionRepository.getRevenueByConnector(ownerId, since);
        List<ConnectorRevenueDTO> connectorRevenue = new ArrayList<>();
        if (connectorRaw != null) {
            connectorRevenue = connectorRaw.stream()
                    .map(row -> new ConnectorRevenueDTO(
                            row[0] != null ? row[0].toString() : "Unknown", 
                            row[1] != null ? ((Number) row[1]).doubleValue() : 0.0
                    ))
                    .collect(Collectors.toList());
        }

        return new AnalyticsSummaryDTO(efficiency, stationRevenue, connectorRevenue);
    }

    public List<DailyStatsDTO> getRevenueTrends(Long ownerId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days).with(LocalTime.MIN);
        List<Object[]> rawData = sessionRepository.getDailyStatsByOwner(ownerId, since);

        Map<LocalDate, DailyStatsDTO> dataMap = new HashMap<>();
        if (rawData != null) {
            for (Object[] row : rawData) {
                LocalDate date;
                if (row[0] instanceof java.sql.Date) {
                    date = ((java.sql.Date) row[0]).toLocalDate();
                } else if (row[0] instanceof java.time.LocalDate) {
                    date = (java.time.LocalDate) row[0];
                } else {
                    date = LocalDate.parse(row[0].toString());
                }
                
                dataMap.put(date, new DailyStatsDTO(
                        date,
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                        row[2] != null ? ((Number) row[2]).doubleValue() : 0.0
                ));
            }
        }

        // Fill gaps with 0 values
        List<DailyStatsDTO> filledData = new ArrayList<>();
        LocalDate startDate = since.toLocalDate();
        LocalDate endDate = LocalDate.now();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            filledData.add(dataMap.getOrDefault(date, new DailyStatsDTO(date, 0.0, 0.0)));
        }

        return filledData;
    }

    public List<PeakHourDTO> getPeakUsage(Long ownerId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days).with(LocalTime.MIN);
        List<Object[]> rawData = sessionRepository.getPeakUsageByOwner(ownerId, since);

        Map<Integer, Long> hourMap = new HashMap<>();
        if (rawData != null) {
            for (Object[] row : rawData) {
                if (row[0] != null) {
                    hourMap.put(((Number) row[0]).intValue(), row[1] != null ? ((Number) row[1]).longValue() : 0L);
                }
            }
        }

        List<PeakHourDTO> filledData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            filledData.add(new PeakHourDTO(i, hourMap.getOrDefault(i, 0L)));
        }

        return filledData;
    }
}
