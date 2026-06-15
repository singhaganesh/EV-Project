package com.ganesh.EV_Project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Redis-backed (Upstash) store for email-based owner auth codes:
 *  - registration email-verification OTP, keyed by userId
 *  - login MFA challenge (temp-login token -> userId + OTP)
 *
 * OTPs are stored hashed (BCrypt) and expire via Redis TTL. This is intentionally
 * separate from {@link OtpService} (mobile/SMS, DB-backed) so the existing app
 * login is left untouched.
 */
@Service
public class MfaOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REG_OTP_PREFIX = "regotp:";
    private static final String MFA_PREFIX = "mfa:";
    private static final String RESET_PREFIX = "pwdreset:";

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.auth.otp-expiration-minutes:5}")
    private long otpExpirationMinutes;

    private String randomOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private Duration ttl() {
        return Duration.ofMinutes(otpExpirationMinutes);
    }

    // ---- Registration email verification ----

    /** Generates a registration OTP for the user, stores its hash with TTL, returns the plaintext. */
    public String generateRegistrationOtp(Long userId) {
        String otp = randomOtp();
        redis.opsForValue().set(REG_OTP_PREFIX + userId, passwordEncoder.encode(otp), ttl());
        return otp;
    }

    /** Validates and consumes a registration OTP. */
    public boolean validateRegistrationOtp(Long userId, String otp) {
        String key = REG_OTP_PREFIX + userId;
        String hash = redis.opsForValue().get(key);
        if (hash == null) return false;
        if (!passwordEncoder.matches(otp, hash)) return false;
        redis.delete(key);
        return true;
    }

    // ---- Password reset ----

    /** Generates a password-reset OTP keyed by email, stores its hash with a 10-minute TTL, returns the plaintext. */
    public String generateResetOtp(String email) {
        String otp = randomOtp();
        redis.opsForValue().set(RESET_PREFIX + email, passwordEncoder.encode(otp), Duration.ofMinutes(10));
        return otp;
    }

    /** Validates and consumes a password-reset OTP. */
    public boolean validateResetOtp(String email, String otp) {
        String key = RESET_PREFIX + email;
        String hash = redis.opsForValue().get(key);
        if (hash == null) return false;
        if (!passwordEncoder.matches(otp, hash)) return false;
        redis.delete(key);
        return true;
    }

    // ---- MFA login challenge ----

    /** Creates a temp-login session + OTP; returns the temp token and the plaintext OTP. */
    public MfaChallenge createMfaChallenge(Long userId) {
        String tempToken = UUID.randomUUID().toString();
        String otp = randomOtp();
        String key = MFA_PREFIX + tempToken;
        redis.opsForHash().putAll(key, Map.of(
                "userId", String.valueOf(userId),
                "otp", passwordEncoder.encode(otp)));
        redis.expire(key, ttl());
        return new MfaChallenge(tempToken, otp);
    }

    /** Regenerates a new MFA OTP for an active temp-login token. Returns the new plaintext OTP, or null if session is invalid. */
    public String regenerateMfaOtp(String tempToken) {
        String key = MFA_PREFIX + tempToken;
        Object userIdObj = redis.opsForHash().get(key, "userId");
        if (userIdObj == null) return null;
        String otp = randomOtp();
        redis.opsForHash().put(key, "otp", passwordEncoder.encode(otp));
        redis.expire(key, ttl());
        return otp;
    }

    /** Retrieves the userId for a temp-login token without consuming/deleting the session. Returns null if expired. */
    public Long getUserIdFromTempToken(String tempToken) {
        String key = MFA_PREFIX + tempToken;
        Object userIdObj = redis.opsForHash().get(key, "userId");
        if (userIdObj == null) return null;
        return Long.parseLong(userIdObj.toString());
    }

    /**
     * Validates the OTP for a temp-login token. Returns the userId on success
     * (consuming the challenge), or null if the token/OTP is invalid or expired.
     */
    public Long validateMfaOtp(String tempToken, String otp) {
        String key = MFA_PREFIX + tempToken;
        Object userIdObj = redis.opsForHash().get(key, "userId");
        Object hashObj = redis.opsForHash().get(key, "otp");
        if (userIdObj == null || hashObj == null) return null;
        if (!passwordEncoder.matches(otp, hashObj.toString())) return null;
        redis.delete(key);
        return Long.parseLong(userIdObj.toString());
    }

    /** Result of creating an MFA challenge: the temp-login token and the plaintext OTP to deliver. */
    public record MfaChallenge(String tempToken, String otp) {}
}
