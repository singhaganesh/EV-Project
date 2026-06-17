package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.VehicleRequest;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.model.Vehicle;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.VehicleRepository;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vehicle garage for the authenticated user (C1).
 */
@RestController
@RequestMapping("/api/users/me/vehicles")
public class VehicleController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();
        List<Vehicle> vehicles = vehicleRepository.findByUserId(user.getId());
        return ResponseEntity.ok(APIResponse.builder().success(true).data(vehicles).build());
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody VehicleRequest request, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();
        if (request.getMake() == null || request.getMake().isBlank()
                || request.getModel() == null || request.getModel().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(APIResponse.builder().success(false).message("Make and model are required").build());
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(user.getId());
        apply(vehicle, request);
        Vehicle saved = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(APIResponse.builder().success(true).message("Vehicle added").data(saved).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleRequest request,
                                    Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle == null || !vehicle.getUserId().equals(user.getId())) {
            return forbiddenOrNotFound(vehicle == null);
        }
        apply(vehicle, request);
        Vehicle saved = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(APIResponse.builder().success(true).message("Vehicle updated").data(saved).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle == null || !vehicle.getUserId().equals(user.getId())) {
            return forbiddenOrNotFound(vehicle == null);
        }
        vehicleRepository.delete(vehicle);
        return ResponseEntity.ok(APIResponse.builder().success(true).message("Vehicle removed").build());
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        if (request.getMake() != null) vehicle.setMake(request.getMake().trim());
        if (request.getModel() != null) vehicle.setModel(request.getModel().trim());
        vehicle.setBatteryKwh(request.getBatteryKwh());
        vehicle.setConnectorType(request.getConnectorType());
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.builder().success(false).message("Unauthorized").build());
    }

    private ResponseEntity<?> forbiddenOrNotFound(boolean notFound) {
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(APIResponse.builder().success(false)
                        .message(notFound ? "Vehicle not found" : "Not allowed").build());
    }
}
