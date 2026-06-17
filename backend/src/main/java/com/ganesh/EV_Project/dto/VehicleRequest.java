package com.ganesh.EV_Project.dto;

import lombok.Data;

/** Payload for creating/updating a vehicle (C1). */
@Data
public class VehicleRequest {
    private String make;
    private String model;
    private Double batteryKwh;
    private String connectorType;
}
