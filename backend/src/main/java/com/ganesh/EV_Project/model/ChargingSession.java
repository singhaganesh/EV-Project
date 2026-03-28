package com.ganesh.EV_Project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "charging_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to booking
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
        "hibernateLazyInitializer", 
        "handler", 
        "chargingSession",
        "user",
        "slot"
    })
    private Booking booking;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private Double energyKwh;       // total energy consumed

    @Column
    private Double totalCost;       // final billed amount

    @Column
    private String status;          // e.g., ONGOING, COMPLETED
}

