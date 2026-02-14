package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.service.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-intent/{bookingId}")
    public ResponseEntity<?> createPaymentIntent(@PathVariable Long bookingId) {
        try {
            Map<String, String> response = paymentService.createPaymentIntent(bookingId);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Payment intent created")
                    .data(response)
                    .build());
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Failed to create payment intent: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, 
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        // In production, verify webhook signature
        // For now, simplified handling
        try {
            // Parse the event and handle different payment statuses
            // This is simplified - in production use Stripe's webhook verification
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long bookingId) {
        try {
            var payment = paymentService.getPaymentByBookingId(bookingId);
            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .data(payment)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
