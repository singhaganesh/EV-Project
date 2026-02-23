package com.ganesh.EV_Project.dto;

import lombok.*;
import java.util.List;

/**
 * Combined viewport response: full data for nearby stations + minimal pins for
 * the rest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewportResponseDTO {
    private List<StationScoreDTO> nearbyStations; // full data, top N
    private List<StationPinDTO> otherPins; // lat/lng only, rest
}
