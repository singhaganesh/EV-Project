package com.ganesh.EV_Project.model;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dispensaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispensary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Station station;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double totalPowerKw;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean acceptsTrucks;

    /** Source of truth for connector type. Stored in dispensaries table. */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ConnectorType connectorType = ConnectorType.CCS2;

    /** Number of charging guns on this dispensary (1 or 2). */
    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfGuns = 2;

    @Column
    private java.time.LocalDateTime lastUsedTime;
}
