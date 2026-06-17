package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.BookingTemplateRequest;
import com.ganesh.EV_Project.model.BookingTemplate;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.BookingTemplateRepository;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Manage recurring-booking templates for the authenticated user (G2).
 */
@RestController
@RequestMapping("/api/users/me/booking-templates")
public class BookingTemplateController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();
        List<BookingTemplate> templates = templateRepository.findByUserId(user.getId());
        return ResponseEntity.ok(APIResponse.builder().success(true).data(templates).build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookingTemplateRequest request, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        String error = validate(request);
        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(APIResponse.builder().success(false).message(error).build());
        }

        BookingTemplate t = new BookingTemplate();
        t.setUserId(user.getId());
        apply(t, request);
        BookingTemplate saved = templateRepository.save(t);
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Recurring booking saved").data(saved).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BookingTemplateRequest request,
                                    Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        BookingTemplate t = templateRepository.findById(id).orElse(null);
        if (t == null || !t.getUserId().equals(user.getId())) {
            return forbiddenOrNotFound(t == null);
        }
        // Toggle-only updates (just active) are allowed without full validation.
        if (request.getActive() != null) t.setActive(request.getActive());
        if (request.getStationId() != null || request.getTimeOfDay() != null
                || request.getDaysOfWeek() != null || request.getConnectorType() != null
                || request.getVehicleType() != null) {
            String error = validate(request);
            if (error != null) {
                return ResponseEntity.badRequest()
                        .body(APIResponse.builder().success(false).message(error).build());
            }
            apply(t, request);
        }
        BookingTemplate saved = templateRepository.save(t);
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Recurring booking updated").data(saved).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        BookingTemplate t = templateRepository.findById(id).orElse(null);
        if (t == null || !t.getUserId().equals(user.getId())) {
            return forbiddenOrNotFound(t == null);
        }
        templateRepository.delete(t);
        return ResponseEntity.ok(APIResponse.builder().success(true).message("Recurring booking removed").build());
    }

    private String validate(BookingTemplateRequest r) {
        if (r.getStationId() == null) return "Station is required";
        if (r.getConnectorType() == null || r.getConnectorType().isBlank()) return "Connector is required";
        if (r.getVehicleType() == null || r.getVehicleType().isBlank()) return "Vehicle type is required";
        if (r.getTimeOfDay() == null || r.getTimeOfDay().isBlank()) return "Time is required";
        if (r.getDaysOfWeek() == null || r.getDaysOfWeek().isBlank()) return "Pick at least one day";
        try {
            LocalTime.parse(r.getTimeOfDay());
        } catch (Exception e) {
            return "Invalid time format (expected HH:mm)";
        }
        return null;
    }

    private void apply(BookingTemplate t, BookingTemplateRequest r) {
        if (r.getStationId() != null) t.setStationId(r.getStationId());
        t.setVehicleId(r.getVehicleId());
        if (r.getConnectorType() != null) t.setConnectorType(r.getConnectorType());
        if (r.getVehicleType() != null) t.setVehicleType(r.getVehicleType());
        if (r.getTimeOfDay() != null) t.setTimeOfDay(LocalTime.parse(r.getTimeOfDay()));
        if (r.getDaysOfWeek() != null) t.setDaysOfWeek(r.getDaysOfWeek().toUpperCase());
        if (r.getActive() != null) t.setActive(r.getActive());
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.builder().success(false).message("Unauthorized").build());
    }

    private ResponseEntity<?> forbiddenOrNotFound(boolean notFound) {
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(APIResponse.builder().success(false)
                        .message(notFound ? "Template not found" : "Not allowed").build());
    }
}
