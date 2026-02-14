package com.ganesh.EV_Project.repository;


import com.ganesh.EV_Project.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByMobileNumberOrderByExpiryTimeDesc(String mobileNumber);
}

