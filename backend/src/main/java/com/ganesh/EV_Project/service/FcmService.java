package com.ganesh.EV_Project.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Low-level Firebase Cloud Messaging sender (CV-11).
 *
 * Relies on the default Firebase app initialized by {@link com.ganesh.EV_Project.config.FirebaseConfig}.
 * If no credentials were configured (so Firebase never initialized), the service
 * reports itself disabled and {@link #sendToToken} becomes a no-op — so the
 * backend boots and all endpoints work even before the service-account key is provided.
 */
@Service
@Slf4j
public class FcmService {

    public enum Result { SENT, INVALID_TOKEN, FAILED, DISABLED }

    public boolean isEnabled() {
        return !FirebaseApp.getApps().isEmpty();
    }

    /**
     * Sends a data-only message so the Android client always builds the
     * notification itself (foreground and background) and can act on the
     * deep link. Returns a {@link Result} so the caller can prune dead tokens.
     */
    public Result sendToToken(String token, String type, String title, String body, String deepLink) {
        if (!isEnabled()) return Result.DISABLED;
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .putData("type", nz(type))
                    .putData("title", nz(title))
                    .putData("body", nz(body))
                    .putData("deepLink", nz(deepLink))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
            return Result.SENT;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Pruning invalid FCM token: {}", code);
                return Result.INVALID_TOKEN;
            }
            log.warn("FCM send failed ({}): {}", code, e.getMessage());
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("FCM send error: {}", e.getMessage());
            return Result.FAILED;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
