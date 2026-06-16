package com.ganesh.EV_Project.dto;

import lombok.Data;

/** Payload for registering an FCM device token (CV-11). */
@Data
public class DeviceTokenRequest {
    private String deviceToken;
    private String platform;
}
