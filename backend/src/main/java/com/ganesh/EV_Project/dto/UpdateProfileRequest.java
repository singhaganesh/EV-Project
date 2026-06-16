package com.ganesh.EV_Project.dto;

import lombok.Data;

/** Payload for editing the user's profile (CV-8b). */
@Data
public class UpdateProfileRequest {
    private String name;
    private String email;
}
