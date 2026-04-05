package com.ganesh.EV_Project.dto;

public record EfficiencyMetricsDTO(
    Double avgDurationMinutes,
    Double avgRevenue,
    Double avgEnergy
) {}
