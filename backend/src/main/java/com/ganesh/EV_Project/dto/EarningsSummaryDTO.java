package com.ganesh.EV_Project.dto;

public record EarningsSummaryDTO(
    Double currentBalance,
    Double lifetimeRevenue,
    Double pendingPayouts,
    Double lastSettlement
) {}
