package com.ganesh.EV_Project.dto;

import lombok.*;

/**
 * Ultra-lightweight DTO for map pins — only coordinates needed to place a
 * marker.
 * Used for non-nearby stations in the viewport response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationPinDTO {
    private Long id;
    private double latitude;
    private double longitude;
}
