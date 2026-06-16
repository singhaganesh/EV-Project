package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.DeviceTokenRequest;
import com.ganesh.EV_Project.dto.UpdateProfileRequest;
import com.ganesh.EV_Project.enums.UserStatus;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.PushNotificationService;
import com.ganesh.EV_Project.service.RefreshTokenService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.builder()
                .success(false).message("Not authenticated").build());
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.builder()
                .success(false).message("Access denied: this resource does not belong to you").build());
    }

    /** Registers (or refreshes) this device's FCM token for the authenticated user (CV-11). */
    @PostMapping("/device-token")
    public ResponseEntity<?> registerDeviceToken(@RequestBody DeviceTokenRequest request,
                                                 Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();
        if (request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false).message("deviceToken is required").build());
        }
        pushNotificationService.registerToken(user.getId(), request.getDeviceToken(), request.getPlatform());
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Device token registered").build());
    }

    /**
     * Deletes the authenticated user's account (CV-8a): PII is anonymized and the
     * mobile/email are freed for re-registration, while financial rows (bookings,
     * sessions, payments) are retained for tax/audit. Refresh + device tokens are purged.
     */
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteMyAccount(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        // Revoke sessions and stop future pushes.
        try {
            refreshTokenService.deleteByUserId(user.getId());
        } catch (Exception ignored) {
            // No active refresh token; nothing to revoke.
        }
        pushNotificationService.deleteTokensForUser(user.getId());

        // Anonymize PII; free unique identifiers so the number/email can be reused.
        user.setName("Deleted User");
        user.setEmail(null);
        user.setMobileNumber("deleted_" + user.getId());
        user.setPassword(null);
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        user.setStatus(UserStatus.DELETED);
        userService.updateUser(user);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Account deleted").build());
    }

    /** Updates the user's editable profile fields (CV-8b). Owner or admin only. */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id,
                                           @RequestBody UpdateProfileRequest request,
                                           Authentication authentication) {
        User caller = userService.getAuthenticatedUser(authentication);
        if (caller == null) return unauthorized();
        if (caller.getRole() != User.Role.ADMIN && !caller.getId().equals(id)) {
            return forbidden();
        }

        User target = userService.findById(id);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.builder()
                    .success(false).message("User not found").build());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            target.setName(request.getName().trim());
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isEmpty()) {
                target.setEmail(null);
            } else {
                User existing = userService.findByEmail(email);
                if (existing != null && !existing.getId().equals(target.getId())) {
                    return ResponseEntity.badRequest().body(APIResponse.builder()
                            .success(false).message("Email already in use").build());
                }
                target.setEmail(email);
            }
        }

        User saved = userService.updateUser(target);
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Profile updated").data(saved).build());
    }
}
