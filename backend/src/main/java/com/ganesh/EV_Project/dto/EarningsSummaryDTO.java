package com.ganesh.EV_Project.dto;

public record EarningsSummaryDTO(
    Double lifetimeRevenue,
    Double energyCost,
    Double netMargin,
    Double revenueLast48h
) {}
