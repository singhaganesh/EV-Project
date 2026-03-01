package com.ganesh.EV_Project.model;

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
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Station station;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double totalPowerKw;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean acceptsTrucks;
}
