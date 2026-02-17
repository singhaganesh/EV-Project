package com.ganesh.EV_Project.enums;

/**
 * Represents the real-time status of a charger slot.
 */
public enum SlotStatus {
    AVAILABLE, // slot free and ready to book
    RESERVED, // reserved for a user but not started
    BOOKED, // booked and active
    CHARGING, // currently charging
    MAINTENANCE, // under maintenance or offline
    OCCUPIED // occupied by non-EV or unauthorized
}
