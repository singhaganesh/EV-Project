package com.ganesh.EV_Project.dto;

import java.time.LocalDate;

public record DailyStatsDTO(
    LocalDate date, 
    Double revenue, 
    Double energy
) {}
