package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A vehicle saved by a user (C1). Powers accurate remaining-time estimates and
 * pre-selecting the connector at booking. Stored against the user id.
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "battery_kwh")
    private Double batteryKwh;

    @Column(name = "connector_type")
    private String connectorType; // CCS2 | TYPE_2

    @CreationTimestamp
    private LocalDateTime createdAt;
}
