package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Otp;
import com.ganesh.EV_Project.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;



    public String generateOtp(String mobileNumber) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        Otp otpEntity = new Otp();
        otpEntity.setMobileNumber(mobileNumber);
        otpEntity.setOtp(otp);
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

        if (!savedOtp.getOtp().equals(otp)) {
            return false; // wrong
        }

        savedOtp.setUsed(true);
        otpRepository.save(savedOtp);

        return true;
    }
}

