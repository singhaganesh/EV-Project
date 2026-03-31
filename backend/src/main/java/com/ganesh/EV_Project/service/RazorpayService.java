package com.ganesh.EV_Project.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws RazorpayException {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    /**
     * Creates a Razorpay Order
     * @param amount Amount in Rupees
     * @param bookingId The internal booking ID
     * @return Razorpay Order ID
     */
    public String createOrder(Double amount, String bookingId) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        // Razorpay expects amount in paise (1 INR = 100 paise)
        orderRequest.put("amount", Math.round(amount * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "booking_" + bookingId);
        
        Order order = client.orders.create(orderRequest);
        return order.get("id");
    }

    /**
     * Verifies the Razorpay payment signature
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }
}
