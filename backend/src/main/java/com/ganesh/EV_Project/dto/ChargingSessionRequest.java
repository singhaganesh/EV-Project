package com.ganesh.EV_Project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChargingSessionRequest {
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}
