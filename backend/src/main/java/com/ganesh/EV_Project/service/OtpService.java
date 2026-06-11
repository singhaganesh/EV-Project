package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Otp;
import com.ganesh.EV_Project.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String generateOtp(String mobileNumber) {

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        Otp otpEntity = new Otp();
        otpEntity.setMobileNumber(mobileNumber);
        // Store only a hash; the plaintext is returned once for delivery (SMS/dev).
        otpEntity.setOtp(passwordEncoder.encode(otp));
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // 5 min validity
        otpRepository.save(otpEntity);

        return otp;
    }

    public boolean validateOtp(String mobileNumber, String otp) {
        Optional<Otp> latestOtp = otpRepository.findTopByMobileNumberOrderByExpiryTimeDesc(mobileNumber);

        if (latestOtp.isEmpty()) {
            return false;
        }

        Otp savedOtp = latestOtp.get();

        if (savedOtp.getUsed()) return false;

        if (savedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false; // expired
        }

        if (!passwordEncoder.matches(otp, savedOtp.getOtp())) {
            loginAttemptService.loginFailed(mobileNumber);
            return false; // wrong
        }

        loginAttemptService.loginSucceeded(mobileNumber);
        savedOtp.setUsed(true);
        otpRepository.save(savedOtp);

        return true;
    }
}

