package com.ganesh.EV_Project.dto;

import com.ganesh.EV_Project.model.Station;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationScoreDTO {

    private Station station;
    private Double distance; // km (Haversine, Eq. 2)
    private Double score; // Composite score S_i (Eq. 3)
    private Double trafficScore; // T_i
    private Double gridScore; // P_i
    private Double parkingScore; // K_i
    private Double accessScore; // A_i
    private Integer availableSlots;
    private Integer totalSlots;
    private List<String> connectorTypes;
    private String lastActive; // e.g. "10 min ago"
    private Double rating; // 0.0 to 5.0
}
