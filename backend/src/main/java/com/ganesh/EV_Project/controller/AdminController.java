package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.enums.UserStatus;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    /** Approves a station owner whose email is verified and pending admin approval. */
    @PutMapping("/users/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveOwner(@PathVariable Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.builder()
                    .success(false)
                    .message("User not found")
                    .build());
        }

        if (user.getRole() != User.Role.STATION_OWNER) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("User is not a station owner")
                    .build());
        }

        user.setStatus(UserStatus.APPROVED);
        userService.updateUser(user);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .message("Pump Owner approved successfully.")
                .build());
    }
}
