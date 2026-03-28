package com.ganesh.EV_Project.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final int LOCK_TIME_MINUTES = 15;

    // Track attempts: <Key (Mobile or IP), AttemptData>
    private final Map<String, AttemptData> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        AttemptData data = attemptsCache.getOrDefault(key, new AttemptData(0, null));
        data.attempts++;
        data.lastModified = LocalDateTime.now();
        attemptsCache.put(key, data);
    }

    public boolean isBlocked(String key) {
        AttemptData data = attemptsCache.get(key);
        if (data == null) {
            return false;
        }

        if (data.attempts >= MAX_ATTEMPTS) {
            // Check if lock has expired
            if (data.lastModified.plusMinutes(LOCK_TIME_MINUTES).isBefore(LocalDateTime.now())) {
                attemptsCache.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    public int getRemainingAttempts(String key) {
        AttemptData data = attemptsCache.get(key);
        if (data == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - data.attempts);
    }

    private static class AttemptData {
        int attempts;
        LocalDateTime lastModified;

        AttemptData(int attempts, LocalDateTime lastModified) {
            this.attempts = attempts;
            this.lastModified = lastModified;
        }
    }
}
