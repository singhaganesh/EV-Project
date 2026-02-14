package com.ganesh.EV_Project.model;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "charger_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many slots belong to one station
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private String slotLabel;       // e.g., "Slot-A1"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SlotType slotType;        // e.g., "AC", "DC"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SlotStatus status;          // e.g., "AVAILABLE", "OCCUPIED", "MAINTENANCE"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConnectorType connectorType;   // e.g., "CCS2", "CHAdeMO"

    @Column(nullable = false)
    private Double powerKw;         // e.g., 22.0

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version                     // for optimistic locking
    private Long version;
}

