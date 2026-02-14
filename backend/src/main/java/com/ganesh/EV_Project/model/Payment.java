package com.ganesh.EV_Project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String currency = "INR";
    
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
    }
}
