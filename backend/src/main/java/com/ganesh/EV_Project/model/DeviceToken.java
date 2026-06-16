package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * An FCM registration token for one device of one user (CV-11). A user may have
 * several devices, and a device's token is unique, so registration upserts by
 * token. Tokens are stored as a plain userId (not a relation) to keep account
 * deletion simple.
 */
@Entity
@Table(name = "device_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(length = 20)
    private String platform;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
