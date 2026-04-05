package com.ganesh.EV_Project.dto;

import java.util.List;

public record AnalyticsSummaryDTO(
    EfficiencyMetricsDTO efficiency,
    List<StationRevenueDTO> stationRevenue,
    List<ConnectorRevenueDTO> connectorRevenue
) {}
