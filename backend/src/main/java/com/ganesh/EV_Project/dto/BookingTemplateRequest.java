package com.ganesh.EV_Project.dto;

import lombok.Data;

/** Payload for creating/updating a recurring-booking template (G2). */
@Data
public class BookingTemplateRequest {
    private Long stationId;
    private Long vehicleId;
    private String connectorType; // CCS2 | TYPE_2
    private String vehicleType;   // CAR | TRUCK
    private String timeOfDay;     // "HH:mm"
    private String daysOfWeek;    // CSV, e.g. "MON,TUE,WED"
    private Boolean active;
}
