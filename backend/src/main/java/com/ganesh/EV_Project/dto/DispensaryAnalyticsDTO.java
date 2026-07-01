package com.ganesh.EV_Project.dto;

import java.util.List;

public record DispensaryAnalyticsDTO(
    Long dispensaryId,
    Long totalSessions,
    Double totalEnergyKwh,
    Double avgDurationMinutes,
    List<PeakHourDTO> peakHours
) {}
