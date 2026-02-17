package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String address;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(columnDefinition = "TEXT")
    private String meta;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double rating;

    @Column
    private java.time.LocalDateTime lastUsedTime;
}
