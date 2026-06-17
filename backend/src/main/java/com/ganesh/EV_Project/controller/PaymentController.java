package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Payment;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.PaymentRepository;
import com.ganesh.EV_Project.service.RazorpayService;
import com.ganesh.EV_Project.service.ReceiptService;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ChargerSlotRepository slotRepository;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private UserService userService;

    /**
     * Streams a payment-receipt PDF for a paid session. Only the session's own
     * customer (or an admin) may download it.
     */
    @GetMapping("/{sessionId}/receipt")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long sessionId, Authentication authentication) {
        User caller = userService.getAuthenticatedUser(authentication);
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.builder().success(false).message("Unauthorized").build());
        }

        ChargingSession session = sessionRepository.findByIdWithDetails(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.builder().success(false).message("Session not found").build());
        }

        Booking booking = session.getBooking();
        Long ownerId = (booking != null && booking.getUser() != null) ? booking.getUser().getId() : null;
        boolean allowed = caller.getRole() == User.Role.ADMIN || (ownerId != null && ownerId.equals(caller.getId()));
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.builder().success(false).message("Not allowed").build());
        }

        if (!"PAID".equals(session.getPaymentStatus())) {
            return ResponseEntity.badRequest()
                    .body(APIResponse.builder().success(false)
                            .message("Receipt is available after payment is completed").build());
        }

        Payment payment = booking != null
                ? paymentRepository.findByBookingId(booking.getId()).orElse(null)
                : null;
        byte[] pdf = receiptService.generate(session, payment);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plugsy-receipt-" + sessionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");
        String sessionIdStr = data.get("sessionId");

        boolean isValid = razorpayService.verifySignature(orderId, paymentId, signature);

        if (isValid) {
            ChargingSession session = null;
            
            // Try lookup by orderId first (Preferred)
            if (orderId != null) {
                session = sessionRepository.findByRazorpayOrderId(orderId).orElse(null);
            }
            
            // Fallback to explicit sessionId
            if (session == null && sessionIdStr != null) {
                session = sessionRepository.findById(Long.parseLong(sessionIdStr)).orElse(null);
            }

            if (session == null) {
                throw new RuntimeException("Session not found for verification");
            }

            // Idempotency: if this session is already paid, return success without
            // re-writing (handles client retries / duplicate webhook deliveries).
            if ("PAID".equals(session.getPaymentStatus())) {
                return ResponseEntity.ok(APIResponse.builder()
                        .success(true)
                        .message("Payment already verified")
                        .data(session)
                        .build());
            }

            // 1. Update Session
            session.setPaymentStatus("PAID");
            sessionRepository.save(session);

            // Release the held slot now that payment is confirmed
            ChargerSlot slot = session.getBooking() != null ? session.getBooking().getSlot() : null;
            if (slot != null && slot.getStatus() == SlotStatus.PAYMENT_PENDING) {
                slot.setStatus(SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }

            // 2. Create and Save Payment Record
            Payment payment = paymentRepository.findByBookingId(session.getBooking().getId())
                    .orElse(new Payment());
            
            payment.setBooking(session.getBooking());
            payment.setAmount(com.ganesh.EV_Project.util.MoneyUtil.round2(
                    session.getTotalCost() != null ? session.getTotalCost() : 0.0));
            payment.setCurrency("INR");
            payment.setTransactionId(paymentId);
            payment.setGateway("RAZORPAY");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            
            paymentRepository.save(payment);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Payment verified and recorded successfully")
                    .data(session) // Return the session here
                    .build());
        } else {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Invalid payment signature")
                    .build());
        }
    }
}
