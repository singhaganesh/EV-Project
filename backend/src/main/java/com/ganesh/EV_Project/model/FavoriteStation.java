package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A user's saved/favorite station (F3). Stored as plain id columns (not entity
 * relations) to keep lookups and inserts simple; a unique constraint prevents
 * duplicate favorites.
 */
@Entity
@Table(name = "favorite_stations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "station_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
