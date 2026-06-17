package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.ReviewRequest;
import com.ganesh.EV_Project.model.Review;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.ReviewRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Station reviews (F2): list with summary, and post (gated on a completed
 * session). Writing recomputes the station's aggregate {@code rating} so it
 * feeds the recommendation score.
 */
@RestController
@RequestMapping("/api/stations/{stationId}/reviews")
public class ReviewController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    /** List reviews + summary; includes whether the caller may post. */
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long stationId, Authentication authentication) {
        List<Review> reviews = reviewRepository.findByStationIdOrderByCreatedAtDesc(stationId);
        Double avg = reviewRepository.averageRating(stationId);
        long count = reviewRepository.countByStationId(stationId);

        boolean canReview = false;
        boolean alreadyReviewed = false;
        User user = userService.getAuthenticatedUser(authentication);
        if (user != null) {
            canReview = sessionRepository.hasCompletedSession(user.getId(), stationId);
            alreadyReviewed = reviewRepository.findByUserIdAndStationId(user.getId(), stationId).isPresent();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("reviews", reviews);
        data.put("average", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        data.put("count", count);
        data.put("canReview", canReview);
        data.put("alreadyReviewed", alreadyReviewed);

        return ResponseEntity.ok(APIResponse.builder().success(true).data(data).build());
    }

    /** Create or update the caller's review for this station. */
    @PostMapping
    public ResponseEntity<?> post(@PathVariable Long stationId,
                                  @RequestBody ReviewRequest request,
                                  Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.builder().success(false).message("Unauthorized").build());
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest()
                    .body(APIResponse.builder().success(false).message("Rating must be 1–5").build());
        }

        // Gate: only customers who have completed a session here may review.
        if (!sessionRepository.hasCompletedSession(user.getId(), stationId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.builder().success(false)
                            .message("You can review a station only after charging there").build());
        }

        Review review = reviewRepository.findByUserIdAndStationId(user.getId(), stationId)
                .orElseGet(Review::new);
        review.setUserId(user.getId());
        review.setStationId(stationId);
        review.setUserName(user.getName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);

        recomputeStationRating(stationId);

        return ResponseEntity.ok(APIResponse.builder()
                .success(true).message("Review submitted").data(review).build());
    }

    /** Recompute and persist the station's aggregate rating after a write. */
    private void recomputeStationRating(Long stationId) {
        Double avg = reviewRepository.averageRating(stationId);
        Station station = stationRepository.findById(stationId).orElse(null);
        if (station != null) {
            station.setRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            stationRepository.save(station);
        }
    }
}
