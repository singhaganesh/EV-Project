package com.ganesh.EV_Project.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationMarkerDTO {
    private Long id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean available; // true if at least 1 slot is free
}
