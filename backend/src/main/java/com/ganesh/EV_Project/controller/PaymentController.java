package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Payment;
import com.ganesh.EV_Project.payload.APIResponse;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import com.ganesh.EV_Project.repository.PaymentRepository;
import com.ganesh.EV_Project.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");

        boolean isValid = razorpayService.verifySignature(orderId, paymentId, signature);

        if (isValid) {
            ChargingSession session = sessionRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Session not found for order: " + orderId));
            
            // 1. Update Session
            session.setPaymentStatus("PAID");
            sessionRepository.save(session);

            // 2. Create and Save Payment Record
            Payment payment = paymentRepository.findByBookingId(session.getBooking().getId())
                    .orElse(new Payment());
            
            payment.setBooking(session.getBooking());
            payment.setAmount(session.getTotalCost());
            payment.setCurrency("INR");
            payment.setTransactionId(paymentId);
            payment.setGateway("RAZORPAY");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            
            paymentRepository.save(payment);

            return ResponseEntity.ok(APIResponse.builder()
                    .success(true)
                    .message("Payment verified and recorded successfully")
                    .build());
        } else {
            return ResponseEntity.badRequest().body(APIResponse.builder()
                    .success(false)
                    .message("Invalid payment signature")
                    .build());
        }
    }
}
