package com.ganesh.EV_Project.dto;

import lombok.Data;

/** Payload for posting a station review (F2). */
@Data
public class ReviewRequest {
    private Integer rating; // 1..5
    private String comment;
}
