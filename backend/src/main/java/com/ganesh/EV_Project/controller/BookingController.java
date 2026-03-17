package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.dto.BookingRequest;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.service.UserService;
import com.ganesh.EV_Project.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return new ResponseEntity<>(bookings, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable Long userId, Authentication authentication) {
        // Secure check: Extract user from JWT and compare IDs
        String principal = authentication.getName(); // This could be email OR mobile number
        
        // Robust lookup: try mobile number first (mandatory), then email
        User currentUser = userService.findByPhoneNumber(principal);
        if (currentUser == null) {
            currentUser = userService.findByEmail(principal);
        }

        if (currentUser == null || (!currentUser.getRole().equals("ADMIN") && !currentUser.getId().equals(userId))) {
            return new ResponseEntity<>("Access Denied: You cannot view other users' bookings", HttpStatus.FORBIDDEN);
        }

        List<Booking> bookings = bookingService.getBookingsByUser(userId);
        return new ResponseEntity<>(bookings, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
            Booking savedBooking = bookingService.createBooking(request);
        return new ResponseEntity<>(savedBooking,HttpStatus.CREATED);
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return new ResponseEntity<>("Your booking is canceled successfully",HttpStatus.OK);
    }
}

