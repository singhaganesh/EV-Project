package com.ganesh.EV_Project.enums;

/**
 * Account lifecycle status. Customers and admins are APPROVED on creation; station
 * owners move PENDING_EMAIL_VERIFICATION -> PENDING_ADMIN_APPROVAL -> APPROVED.
 */
public enum UserStatus {
    PENDING_EMAIL_VERIFICATION,
    PENDING_ADMIN_APPROVAL,
    APPROVED,
    SUSPENDED
}
