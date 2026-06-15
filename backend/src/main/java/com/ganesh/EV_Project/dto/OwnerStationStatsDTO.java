package com.ganesh.EV_Project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerStationStatsDTO {
    private Long totalStations;
    private Long activeStationsCount;
    private Long activeChargers;
    private Long inUseChargers;
    private Double utilizationRate;
    private Double todayEnergyKwh;
    private Double todayEarnings;

    // Hour-aligned trend vs. the same window yesterday (percentages).
    private Double energyTrendPercentage;
    private Double earningsTrendPercentage;
}
