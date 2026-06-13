package com.ganesh.EV_Project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails via Gmail SMTP (configured in application-*.properties).
 * Used to deliver owner registration verification and login MFA codes.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * Sends a one-time code to the given address.
     *
     * @param purpose short human label, e.g. "verification" or "login"
     */
    public void sendOtpEmail(String to, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Your Plugsy " + purpose + " code");
        message.setText("Your Plugsy " + purpose + " code is: " + otp
                + "\n\nThis code expires shortly. If you didn't request it, please ignore this email.");
        mailSender.send(message);
    }
}
