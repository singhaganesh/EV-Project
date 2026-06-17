package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A recurring-booking template (G2).
 *
 * Describes a repeating reservation (station + connector + vehicle + time +
 * weekdays). It does NOT hold a future slot — a scheduler materializes a normal
 * 20-minute booking ~20 min before each occurrence, so behavior matches the
 * existing instant-booking flow. {@code lastMaterializedDate} guarantees one
 * attempt per day per template.
 */
@Entity
@Table(name = "booking_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "vehicle_id")
    private Long vehicleId; // informational reference to the user's chosen vehicle

    @Column(name = "connector_type", nullable = false)
    private String connectorType; // CCS2 | TYPE_2

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType; // CAR | TRUCK

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    // CSV of 3-letter uppercase day codes, e.g. "MON,TUE,WED".
    @Column(name = "days_of_week", nullable = false)
    private String daysOfWeek;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_materialized_date")
    private LocalDate lastMaterializedDate;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
