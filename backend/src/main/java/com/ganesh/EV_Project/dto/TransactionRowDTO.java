package com.ganesh.EV_Project.dto;

import java.time.LocalDateTime;

public record TransactionRowDTO(
    Long sessionId,
    LocalDateTime timestamp,
    String stationName,
    Double energyKwh,
    Double amount,
    String razorpayOrderId,
    String status
) {}
