package com.ganesh.EV_Project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.VehicleType;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Station ID is required")
    private Long stationId;

    @NotNull(message = "Connector type is required")
    private ConnectorType connectorType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    // Optional: admin override to force a specific slot
    private Long slotId;

    // Optional: If true, allows booking a truck slot for a car if car slots are full
    private boolean allowTruckSlotFallback = false;
}
