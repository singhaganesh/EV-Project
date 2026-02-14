package com.ganesh.EV_Project.enums;

public enum BookingStatus {
    PENDING,       // booking requested, waiting for confirmation
    CONFIRMED,     // booking confirmed
    ONGOING,       // charging in progress
    COMPLETED,     // charging completed
    EXPIRED,
    CANCELLED;     // booking cancelled
}

