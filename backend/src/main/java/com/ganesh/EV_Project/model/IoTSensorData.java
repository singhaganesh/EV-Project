package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_sensor_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IoTSensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private Double voltage; // V

    @Column(nullable = false)
    private Double current; // A

    @Column(nullable = false)
    private Double power; // W = V × I (Eq. 5)

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
