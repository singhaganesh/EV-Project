package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.DeviceToken;
import com.ganesh.EV_Project.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * High-level push entry point (CV-11). Resolves a user's registered devices,
 * sends the message via {@link FcmService}, and prunes tokens FCM reports as
 * dead. Designed to never throw into a caller's business transaction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;

    /** Upserts a device's FCM token for the given user. */
    @Transactional
    public void registerToken(Long userId, String token, String platform) {
        if (token == null || token.isBlank()) return;
        DeviceToken dt = deviceTokenRepository.findByToken(token).orElseGet(DeviceToken::new);
        dt.setUserId(userId);
        dt.setToken(token);
        dt.setPlatform(platform != null ? platform : "android");
        LocalDateTime now = LocalDateTime.now();
        if (dt.getCreatedAt() == null) dt.setCreatedAt(now);
        dt.setUpdatedAt(now);
        deviceTokenRepository.save(dt);
    }

    @Transactional
    public void deleteTokensForUser(Long userId) {
        deviceTokenRepository.deleteByUserId(userId);
    }

    /**
     * Sends a notification to every device registered to the user. Failures are
     * swallowed; dead tokens are removed. Safe to call from inside other flows.
     */
    public void sendToUser(Long userId, String type, String title, String body, String deepLink) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
            for (DeviceToken dt : tokens) {
                FcmService.Result result =
                        fcmService.sendToToken(dt.getToken(), type, title, body, deepLink);
                if (result == FcmService.Result.INVALID_TOKEN) {
                    deviceTokenRepository.delete(dt);
                }
            }
        } catch (Exception e) {
            log.warn("Push to user {} failed: {}", userId, e.getMessage());
        }
    }
}
