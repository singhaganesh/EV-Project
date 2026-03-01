package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "stations", indexes = {
        @Index(name = "idx_station_lat_lng", columnList = "latitude, longitude")
})
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

    @Column
    private String operatingHours; // e.g. "24 Hours", "6 AM - 10 PM"

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double pricePerKwh; // e.g. 16.5

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double truckPricePerKwh; // e.g. 20.0

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getIsOpen() {
        if (operatingHours == null || "24 Hours".equalsIgnoreCase(operatingHours)) {
            return true;
        }

        try {
            Pattern pattern = Pattern.compile("(\\d{1,2})\\s*(AM|PM)\\s*-\\s*(\\d{1,2})\\s*(AM|PM)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(operatingHours);

            if (!matcher.find()) {
                return true; // Fallback if format doesn't match
            }

            int openHour = Integer.parseInt(matcher.group(1));
            String openPeriod = matcher.group(2).toUpperCase();
            int closeHour = Integer.parseInt(matcher.group(3));
            String closePeriod = matcher.group(4).toUpperCase();

            LocalTime openTime = convertToLocalTime(openHour, openPeriod);
            LocalTime closeTime = convertToLocalTime(closeHour, closePeriod);
            LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            if (closeTime.isAfter(openTime)) {
                return !now.isBefore(openTime) && now.isBefore(closeTime);
            } else if (closeTime.equals(openTime)) {
                return true;
            } else {
                // Wraps around midnight
                return !now.isBefore(openTime) || now.isBefore(closeTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true; // Default safety fallback
        }
    }

    private LocalTime convertToLocalTime(int hour, String period) {
        if (hour == 12) {
            hour = period.equals("AM") ? 0 : 12;
        } else if (period.equals("PM")) {
            hour += 12;
        }
        return LocalTime.of(hour, 0);
    }

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Dispensary> dispensaries = new java.util.ArrayList<>();
}
