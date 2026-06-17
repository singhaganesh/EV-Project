package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.FavoriteStation;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.FavoriteStationRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Saved/favorite stations for the authenticated user (F3).
 */
@RestController
@RequestMapping("/api/users/me/favorites")
public class FavoriteController {

    @Autowired
    private UserService userService;

    @Autowired
    private FavoriteStationRepository favoriteRepository;

    @Autowired
    private StationRepository stationRepository;

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        List<Long> ids = favoriteRepository.findByUserId(user.getId()).stream()
                .map(FavoriteStation::getStationId)
                .toList();
        List<Station> stations = ids.isEmpty() ? List.of() : stationRepository.findAllById(ids);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true).data(stations).build());
    }

    @PostMapping("/{stationId}")
    public ResponseEntity<?> add(@PathVariable Long stationId, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        if (!favoriteRepository.existsByUserIdAndStationId(user.getId(), stationId)) {
            FavoriteStation fav = new FavoriteStation();
            fav.setUserId(user.getId());
            fav.setStationId(stationId);
            favoriteRepository.save(fav);
        }
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Added to favorites").build());
    }

    @DeleteMapping("/{stationId}")
    public ResponseEntity<?> remove(@PathVariable Long stationId, Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) return unauthorized();

        favoriteRepository.findByUserIdAndStationId(user.getId(), stationId)
                .ifPresent(favoriteRepository::delete);
        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Removed from favorites").build());
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.builder().success(false).message("Unauthorized").build());
    }
}
