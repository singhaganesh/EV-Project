package com.ganesh.EV_Project.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIResponse {
    private boolean success;
    private String message;
    private Object data;
    
    public APIResponse(String message, boolean status) {
        this.message = message;
        this.success = status;
    }
}
