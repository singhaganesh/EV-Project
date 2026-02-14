package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.Payment;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public Map<String, String> createPaymentIntent(Long bookingId) throws StripeException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Create payment intent with Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (booking.getPriceEstimate() * 100)) // Convert to cents/paise
                .setCurrency("inr")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("bookingId", bookingId.toString())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // Save payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getPriceEstimate())
                .stripePaymentIntentId(intent.getId())
                .status(Payment.PaymentStatus.PENDING)
                .build();
        
        paymentRepository.save(payment);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        response.put("paymentIntentId", intent.getId());
        return response;
    }

    @Transactional
    public void handlePaymentSuccess(String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Transactional
    public void handlePaymentFailure(String paymentIntentId, String failureReason) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);
    }

    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking"));
    }
}
